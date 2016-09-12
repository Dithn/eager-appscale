dev.new(height=5, width=7.5)
data <- read.table('input_max_pod_performance.txt', header=TRUE)
par(mai=c(1,1,0.7,1))
bars <- barplot(t(as.matrix(cbind(data[,'Memory'], data[,'CPU']*(800/250)))), beside=T, ylim=c(0,800), col=c('red','blue'), names.arg=c(100, 1000, 10000), xlab='Number of Detectors', ylab='Max Memory Usage (MB)', legend=c('Memory','CPU'), args.legend=list(x = 'top'))
axis(4, at = seq(0, 800, length.out = 6), labels = round(seq(0, 250, length.out = 6), 2))
mtext(side=4, 'Max CPU Usage (%)', line = 2.75)

