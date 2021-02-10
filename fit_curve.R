library(minpack.lm)
require(dplyr)
script.dir <- dirname(sys.frame(1)$ofile)
wd <- script.dir
setwd(wd)
main_df <- read.csv('./extraction/target/months_result.csv')
poly_d <- split(main_df, main_df$GeomID)

for (d in poly_d) {
  d2 <- d[order(as.Date(d$date, format="%Y-%m-%dT00:00:00")),]
  d2$time_double <- as.numeric(as.POSIXct(d2$date))
  d2$contributions <- cumsum(d2$contributions)
  d2 <- d2 %>% slice(min(which((d2$contributions!=0) == TRUE)):n())
  d2$idx <- seq.int(nrow(d2))
  d2 <- d2[as.Date(d2$date, format="%Y-%m-%dT00:00:00")>="2007-10-01",]
  
  mo <- nlsLM(d2$contributions~A+B/(1+exp(-C*(d2$idx-D))), data=d2, control=nls.lm.control(maxiter=1000), start=list(A=1, B=max(d2$contributions), C=0.05, D=100))
  coeffs = coefficients(mo)
  d2$pred <- coeffs[1] + coeffs[2] / (1+exp(-coeffs[3]*(d2$idx-coeffs[4])))
  fn <- paste("./data/predictions/z",toString(d2$GeomID[1]), ".csv", sep="")
  write.csv(d2[c("idx", "time_double", "contributions", "pred")], fn, row.names = TRUE)
}
