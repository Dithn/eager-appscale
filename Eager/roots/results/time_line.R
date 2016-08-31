ts_data <- read.table('times.txt', sep=',')
times <- as.POSIXct(ts_data[,1], format='%Y-%m-%d %H:%M:%S')

dev.new(width=6, height=2.5)

start <- as.POSIXct('2016-08-19 12:30:00', format='%Y-%m-%d %H:%M:%S')
end <- as.POSIXct('2016-08-19 22:30:00', format='%Y-%m-%d %H:%M:%S')
range_times <- c(start, end)
ticks <- seq(start, end, by = "hours")
plot(NA, ylim=c(0,1), xlim=range_times, ann=FALSE, axes=FALSE, xlab='Time')
abline(h=0, lwd=2, col="#5B7FA3")

title(xlab='Time (HH:mm)', cex.lab=0.75)
 
axis.POSIXct(1, at=seq.POSIXt(range_times[1], range_times[2], by="hours"), format="%H:%M", cex.axis=0.6, pos=0, lwd=0, lwd.tick=2, col="#5B7FA3", font=2, xlab='Time')

for (hour in c(12:22)) {
    print(hour)
    start2 <- as.POSIXct(paste('2016-08-19 ', hour, ':00:00', sep=''), format='%Y-%m-%d %H:%M:%S')
    end2   <- as.POSIXct(paste('2016-08-19 ', hour, ':02:00', sep=''), format='%Y-%m-%d %H:%M:%S')
    print(start2)
    print(end2)
    axis.POSIXct(3, at=seq.POSIXt(start2, end2, by="min"), format="%H:%M", cex.axis=0.6, pos=0, lwd=0, lwd.tick=2, col="red", lab=NA)
}

for (i in c(1:nrow(ts_data))) {
    point <- as.POSIXct(ts_data[i, 1], format='%Y-%m-%d %H:%M:%S')
    if (ts_data[i,2] == 1) {
       segments(point, 0, point, 1, col='blue')
       #points(point, 0.1, type='o', pch='+')
    } else {
       segments(point, 0, point, 1, col='green')
    }
}