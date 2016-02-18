library('relaimpo')

gae <- read.table('gae_5d_harvest.txt', header=TRUE)
gae_model <- lm(Total ~ ., data=gae)
calc.relimp(gae_model, type=c('lmg'))