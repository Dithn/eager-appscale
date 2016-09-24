ts_data <- read.table('input_time_line_j1j8.txt', sep=',')
times <- as.POSIXct(ts_data[,1], format='%Y-%m-%d %H:%M:%S')

dev.new(width=6, height=1.8)

start <- as.POSIXct('2016-09-09 13:00:00', format='%Y-%m-%d %H:%M:%S')
end <- as.POSIXct('2016-09-09 23:00:00', format='%Y-%m-%d %H:%M:%S')
range_times <- c(start, end)
ticks <- seq(start, end, by = "hours")

par(mar=c(1.5,0,1,0))
plot(NA, ylim=c(-0.4,0.4), xlim=range_times, ann=FALSE, axes=FALSE, xlab='Time')
#abline(h=0, lwd=2, col="#5B7FA3")

title(xlab='Time (hh:mm)', cex.lab=0.9, line=0.1)
 
axis.POSIXct(1, at=seq.POSIXt(range_times[1], range_times[2], by=30 * 60), srt=45, format="%H:%M", cex.axis=0.8, pos=0, lwd=2.2, lwd.tick=2, col="#5B7FA3", font=2, xlab='Time')

for (hour in c(14, 16, 18, 20, 22)) {
    start2 <- as.POSIXct(paste('2016-09-09 ', hour, ':00:00', sep=''), format='%Y-%m-%d %H:%M:%S')
    arrows(start2, -0.17, start2, 0, col='red', length=0.1, angle=25, lwd=2)
}

for (i in c(1:nrow(ts_data))) {
    point <- as.POSIXct(ts_data[i, 1], format='%Y-%m-%d %H:%M:%S')
    if (ts_data[i,2] == 4) {
       points(point, 0.15, type='o', pch=4, col='blue', bg='blue')
    }
    if (ts_data[i,2] == 6) {
       points(point, 0.1, type='o', pch=4, col='darkgreen', bg='darkgreen')
    }
    if (ts_data[i,2] == 7) {
       points(point, 0.05, type='o', pch=4, col='darkmagenta', bg='darkmagenta')
    }
}

legend('top', legend=c('G4 anomaly', 'G6 anomaly', 'G7 anomaly'), pch=c(4,4,4), col=c('blue', 'darkgreen', 'darkmagenta'), horiz=T, cex=0.8, bty='n')
legend('topleft', legend=c('Fault injection'), col=c('red'), lty=1:1, horiz=T, cex=0.8, bty='n', lwd=2)
