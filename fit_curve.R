library(minpack.lm)
script.dir <- dirname(sys.frame(1)$ofile)
wd <- paste(script.dir, '/data/data_by_poly', sep='')
setwd(wd)
inputs <- list.files()
for (inp in inputs) {
  print(inp)
  if (substr(inp[1],1,1)=="z"){
    d <-read.csv(file=inp)
    mo <- nlsLM(d$contributions~A+B/(1+exp(-C*(d$idx-D))), data=d, control=nls.lm.control(maxiter=1000), start=list(A=1, B=max(d$contributions), C=0.05, D=100))
    coeffs = coefficients(mo)
    d$pred <- coeffs[1] + coeffs[2] / (1+exp(-coeffs[3]*(d$idx-coeffs[4])))
    fn <- paste(script.dir, "/data/predictions/",inp, sep='')
    print(fn)
    write.csv(d, fn, row.names = TRUE)
  }
}
