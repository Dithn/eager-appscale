dev.new(height=5, width=7.5)
data <- read.table('input_pod_performance.txt', header=TRUE)
par(mai=c(1,1,0.7,1))
bars <- barplot(t(as.matrix(cbind(data[,'Memory'], data[,'CPU']*7))), beside=T, ylim=c(0,700), col=c('red','blue'), names.arg=c(100, 1000, 10000), xlab='Number of Detectors', ylab='Average Memory Usage (MB)', legend=c('Memory','CPU'), args.legend=list(x = 'top'))
axis(4, at = seq(0, 700, length.out = 11), labels = round(seq(0, 100, length.out = 11), 2))
mtext(side=4, 'Average CPU Usage (%)', line = 2.75)

arrows(bars[1,], as.numeric(data[,2] - 2*data[,3]), bars[1,], as.numeric(data[,2] + 2*data[,3]), lwd = 1.5, angle = 90, code = 3, length = 0.05)
arrows(bars[2,], as.numeric(data[,4] - 2*data[,5]) * 7, bars[2,], as.numeric(data[,4] + 2*data[,5]) * 7, lwd = 1.5, angle = 90, code = 3, length = 0.05)

