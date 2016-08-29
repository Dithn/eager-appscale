ts_data <- read.table('times.txt')
times <- as.POSIXct(ts_data[,1], format='%H:%M:%S')
range_times <- range(times)

plot(NA, ylim=c(-1,1), xlim=range_times, ann=FALSE, axes=FALSE)
abline(h=0, lwd=2, col="#5B7FA3")
 
segments(times, 0, times, ypts, col="gray80")
axis.POSIXct(
  1,
  at=seq.POSIXt(range_times[1], range_times[2], by="min"),
  format="%H:%M",
  cex.axis=0.6,
  pos=0,
  lwd=0,
  lwd.tick=2,
  col="#5B7FA3",
  font=2
)
