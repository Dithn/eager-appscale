library('methods')

relimp_vs_time <- read.table('gae_relimp_over_time.txt', header=T)
anomaly_vs_time <- read.table('gae_anomaly_over_time.txt')

columns = c('bm_datastore_asList_1000', 'bm_datastore_asList_100', 'bm_datastore_asList_10', 'bm_datastore_asSingleEntity', 'bm_datastore_get', 'bm_datastore_put', 'bm_datastore_delete', 'bm_memcache_get', 'bm_memcache_put', 'bm_memcache_delete')
cl <- rainbow(10)

x <- relimp_vs_time$index
plot(x, relimp_vs_time$bm_datastore_asList_1000, type='n', ylim=c(0,1), xlab='Time', ylab='Relative Importance')
for (i in 1:10) {
  lines(x, relimp_vs_time[[columns[i]]], type='l', col=cl[i])
}

legend("topright", legend = columns, col = cl, lwd = 1, cex = 0.5)
for (i in 1:nrow(anomaly_vs_time)) {
  abline(v=anomaly_vs_time[i,2])
}