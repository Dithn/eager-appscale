data <- read.table('input_perf_overhead.txt', sep=',', header=T)
barx <- barplot(as.matrix(data), beside=T, xlab='Application', ylab='Average time per request (ms)', col=c('red','blue'), names.arg=c('guestbook','stock-trader','kv store','cached kv store'), ylim=c(-5,200))
legend("topright", c("Without Roots", "With Roots"), cex=1, bty="n", fill=c('red','blue'))

red_sd <- c(3.9, 13, 1.5, 2.8)
blue_sd <- c(3.7, 13.7, 2.2, 3.3)
arrows(barx[1,], as.numeric(data[1,] - 2*red_sd), barx[1,], as.numeric(data[1,] + 2*red_sd), lwd = 1.5, angle = 90, code = 3, length = 0.05)
arrows(barx[2,], as.numeric(data[2,] - 2*blue_sd), barx[2,], as.numeric(data[2,] + 2*blue_sd), lwd = 1.5, angle = 90, code = 3, length = 0.05)
