library('methods')

args <- commandArgs(trailingOnly=TRUE)
relimp_vs_time <- read.table(args[1], header=T)

column_names <- names(relimp_vs_time)
columns <- column_names[c(3:length(column_names))]
cl <- rainbow(length(columns))

x <- relimp_vs_time$index
plot(x, relimp_vs_time[,3], type='n', xlim=c(0,length(relimp_vs_time[,1])), ylim=c(0,1), xlab='Time', ylab='Relative Importance')
for (i in 1:length(columns)) {
  lines(x, relimp_vs_time[[columns[i]]], type='l', col=cl[i])
}

legend("topright", legend = columns, col = cl, lwd = 1, cex = 0.5)
