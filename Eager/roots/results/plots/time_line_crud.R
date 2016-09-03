ts_data <- read.table('input_time_line_crud.txt', sep=',')
times <- as.POSIXct(ts_data[,1], format='%Y-%m-%d %H:%M:%S')

dev.new(width=6, height=1.8)

start <- as.POSIXct('2016-08-27 14:00:00', format='%Y-%m-%d %H:%M:%S')
end <- as.POSIXct('2016-08-28 00:00:00', format='%Y-%m-%d %H:%M:%S')
range_times <- c(start, end)
ticks <- seq(start, end, by = "hours")

par(mar=c(1.5,0,1,0))
plot(NA, ylim=c(-0.4,0.4), xlim=range_times, ann=FALSE, axes=FALSE, xlab='Time')
#abline(h=0, lwd=2, col="#5B7FA3")

title(xlab='Time (HH:mm)', cex.lab=0.9, line=0.1)
 
axis.POSIXct(1, at=seq.POSIXt(range_times[1], range_times[2], by=30 * 60), srt=45, format="%H:%M", cex.axis=0.8, pos=0, lwd=2.2, lwd.tick=2, col="#5B7FA3", font=2, xlab='Time')

for (i in c(1:nrow(ts_data))) {
    point <- as.POSIXct(ts_data[i, 1], format='%Y-%m-%d %H:%M:%S')
    print(point)
    if (as.character(ts_data[i,2]) == ' I') {
       print(ts_data[i, 2])
       arrows(point, -0.16, point, 0, col='red', length=0.1, angle=25, lwd=2)
    } else {
      arrows(point, 0.16, point, 0, col='blue', length=0.1, angle=25, lwd=2)
    }
}

legend('top', legend=c('Anomalous workload injection', 'Anomaly detection'), col=c('red', 'blue'), lty=1:1, horiz=T, cex=0.8, bty='n', lwd=2)