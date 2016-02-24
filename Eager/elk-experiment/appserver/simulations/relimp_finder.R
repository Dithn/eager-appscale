library('relaimpo')
library('methods')

args <- commandArgs(trailingOnly=TRUE)

gae <- read.table(args[1], header=TRUE)
limit <- quantile(gae$Total, 0.99)

model <- lm(Total ~ ., data=gae)
rankings <- calc.relimp(model, type=c('lmg'))
lmgr <- rankings$lmg
cat('[relimp] index', names(lmgr), '\n')

anomalies <- 0
for (i in 1:nrow(gae)) {
  if (i > 100) {
    model <- lm(Total ~ ., data=gae[1:i,])
    rankings <- calc.relimp(model, type=c('lmg'))
    lmgr <- rankings$lmg
    cat('[relimp]', i, paste(lmgr, ' '), '\n')
  }
  if (limit < gae[i, "Total"]) {
    anomalies <- anomalies + 1
    cat('[anomaly]', i, gae[i, "Total"], "\n")
  }
}
