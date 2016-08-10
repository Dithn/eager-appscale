args <- commandArgs(trailingOnly=TRUE)
actual_data <- read.table(args[1])$V1
generated_data <- rnorm(length(actual_data), mean(actual_data), sd(actual_data))
tt <- t.test(actual_data, generated_data)
print(tt)