import argparse
from os.path import join

import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt


class BamRatioReader(object):
    """
    Reader object for bam_ratio.txt files
    """

    def __init__(self, filename):
        self.filename = filename
        self.__handle = open(filename)


    def __reset_handle(self):
        self.__handle.close()
        self.__handle = open(self.filename)


    def get_chromosome(self, chromosome):
        self.__reset_handle()
        lines = []
        for line in self.__handle:
            if line.split("\t")[0] == chromosome:
                lines.append(line)
        return lines

    @property
    def chromosomes(self):
        self.__reset_handle()
        chrs = []
        for x in self.__handle:
            if "Chromosome" not in x:
                chrs.append(x.split("\t")[0])
        return list(set(chrs))



def get_cn_lines(lines, cn_type="diploid" ,ploidy=2):
    """
    For a list if lines, return those lines belonging to a certain cn_typle
    :param lines: list of lines
    :param cn_type: either "normal" (cn= ploidy), "loss" (cn < ploidy) or "gain" (cn > ploidy)
    :return: list of lines
    """
    n_lines = []
    for line in lines:
        cn = line.strip().split("\t")[-1]
        if cn_type == "normal" and int(cn) == ploidy:
            n_lines.append(line)
        elif cn_type == "loss" and int(cn) < ploidy:
            n_lines.append(line)
        elif cn_type == "gain" and int(cn) > ploidy:
            n_lines.append(line)
    return n_lines


def lines_to_array(lines, ploidy=2):
    """
    Convert list of lines to numpy array of [start, ratio]
    """
    tmp = []
    for x in lines:
        start = x.split("\t")[1]
        ratio = x.split("\t")[2]
        tmp.append([int(start), float(ratio)*ploidy])
    return np.array(tmp)


def plot_chromosome(lines, chromosome, output_file, ploidy):
    """
    Plot lines belonging to a chromosome
    green = where CN = ploidy
    red = where CN > ploidy
    blue = where CN < ploidy
    """
    fig = plt.figure(figsize=(8,6))
    ax = fig.add_subplot(111)
    normals = lines_to_array(get_cn_lines(lines, "normal", ploidy), ploidy)
    losses = lines_to_array(get_cn_lines(lines, "loss", ploidy), ploidy)
    gains = lines_to_array(get_cn_lines(lines, "gain", ploidy), ploidy)
    print("Plotting chromosome {0}".format(chromosome))
    all_x = []

    if len(normals) > 0:
        ax.scatter(normals[:, 0], normals[:,1], color="g")
        for x in normals[:, 0]:
            all_x += [x]
    if len(losses) > 0:
        ax.scatter(losses[:, 0], losses[:,1], color="b")
        for x in losses[:, 0]:
            all_x += [x]
    if len(gains) > 0:
        for x in gains[:, 0]:
            all_x += [x]
        ax.scatter(gains[:,0], gains[:,1], color="r")

    ax.set_ylim(0, ploidy*3)
    ax.set_xlim(int(0-(max(all_x)*0.1)), int(max(all_x)+(max(all_x)*0.1)))

    ax.set_xlabel("chromosome position")
    ax.set_ylabel("CN")
    ax.set_title("Chromosome {0}".format(chromosome))

    plt.savefig(output_file)
    plt.close()



if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-I', "--input", required=True, help="Input bam_ratio.txt file")
    parser.add_argument('-O', '--output-dir', required=True, help="Path to output dir")
    parser.add_argument("-p", "--ploidy", type=int, default=2, help="Ploidy of sample")

    args = parser.parse_args()

    reader = BamRatioReader(args.input)
    for chromosome in reader.chromosomes:
        ofile = join(args.output_dir, "chr{0}.png".format(chromosome))
        lines = reader.get_chromosome(chromosome)
        plot_chromosome(lines, chromosome, ofile, args.ploidy)