library('relaimpo')
library('methods')

gae <- read.table('gae_5d_harvest.txt', header=TRUE)
limit <- quantile(gae$Total, 0.95)
limit2 <- quantile(gae$bm_datastore_asList_1000, 0.95)

model <- lm(Total ~ ., data=gae)
rankings <- calc.relimp(model, type=c('lmg'))
lmgr <- rankings$lmg
cat('[relimp] index', names(lmgr), '\n')

anomalies <- 0
condition_met <- 0
for (i in 1:nrow(gae)) {
  if (i > 100) {
    model <- lm(Total ~ ., data=gae[1:i,])
    rankings <- calc.relimp(model, type=c('lmg'))
    lmgr <- rankings$lmg
    cat('[relimp]', i, paste(lmgr, ' '), '\n')
  }
  if (limit < gae[i, "Total"]) {
    anomalies <- anomalies + 1
    if (limit2 < gae[i, "bm_datastore_asList_1000"]) {
      condition_met <- condition_met + 1
    }
    cat('[anomaly]', i, gae[i, "Total"], gae[i, "bm_datastore_asList_1000"], gae[i, "bm_datastore_asList_1000"] > limit2, "\n")
  }
}

cat('\nPercentage condition met:', condition_met*100.0/anomalies, '\n')