library('dtw')

args <- commandArgs(trailingOnly=TRUE)
data <- read.table(args[1])
pearson_r <- cor(data$V1, data$V2, method='pearson')
time_warp <- dtw(data$V1, data$V2)
cat(pearson_r, time_warp$distance, nrow(data))

