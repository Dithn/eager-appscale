library('relaimpo')
library('methods')

gae <- read.table('gae_5d_harvest.txt', header=TRUE)
gae_model <- lm(Total ~ ., data=gae)
calc.relimp(gae_model, type=c('lmg'))

limit <- quantile(gae$Total, 0.95)
for (i in 1:nrow(gae)) {
  if (limit < gae[i, "Total"]) {
    cat(i, gae[i, "Total"], '\n')
    if (i > 100) {
      model <- lm(Total ~ ., data=gae[1:i,])
      rankings <- calc.relimp(model, type=c('lmg'))
      print(rankings$lmg)
    }
    cat('\n')
  }
}