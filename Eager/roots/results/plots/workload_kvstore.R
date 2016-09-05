workload <- read.table('input_workload_kvstore.txt', sep=',')
time_axis <- as.POSIXct(workload[,1], format='%Y-%m-%d %H:%M:%S')
start <- as.POSIXct('2016-08-30 12:30:00')
end <- as.POSIXct('2016-08-30 22:30:00')

dev.new(height=4.5)
par(mar=c(5,4,2,2))
plot(time_axis, workload[,2], type='l', xlim=c(start,end), xlab='Time (hh:mm)', ylab="Requests per minute", col='blue', lwd=2)

anomalies <- c('2016-08-30 14:13:32', '2016-08-30 14:38:32', '2016-08-30 18:32:32', '2016-08-30 20:14:32', '2016-08-30 20:39:32', '2016-08-30 21:58:32')
abline(v=as.POSIXct(anomalies), col='red', lty=2, lwd=1.5)