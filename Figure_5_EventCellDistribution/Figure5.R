library(sf)
library(tmap)
library(tidyverse)
library(grid)

#load data
countries = st_read(dsn = "./Figure5_Data.gpkg",layer='CountriesSinglePartClean')
countries$CONTINENT<-factor(countries$CONTINENT,levels=c('North America','Europe','Asia','South America','Africa','Oceania'))
cells = st_read(dsn = "./Figure5_Data.gpkg",layer='GridCells')
#dummy values for equal legends between graphs
dummyMax<-st_sf(entities=0,
                id=0,
                weights_Local.knowledge.event=100,
                weights_Remote.event=100,
                weights_Early.import=100,
                weights_Geometry.import=100,
                weights_Late.import=100,
                weights_Tag.import=100,
                geom = st_sfc(st_polygon(list(cbind(c(0,.Machine$double.eps,.Machine$double.eps,0,0),c(0,0,.Machine$double.eps,.Machine$double.eps,0))))),
                crs = st_crs(4326))
dummyMin<-st_sf(entities=0,
                id=0,
                weights_Local.knowledge.event=0,
                weights_Remote.event=0,
                weights_Early.import=0,
                weights_Geometry.import=0,
                weights_Late.import=0,
                weights_Tag.import=0,
                geom = st_sfc(st_polygon(list(cbind(c(0,.Machine$double.eps,.Machine$double.eps,0,0),c(0,0,.Machine$double.eps,.Machine$double.eps,0))))),
                crs = st_crs(4326))
cells<-rbind(cells,dummyMax,dummyMin)

#wide2long (tried facets by event type first but then highlighting sub regions is difficult...)
# not working with sf: cells<-pivot_longer(cells,cols=starts_with('weights'),names_to="EventType",values_to="weight")
#cells<-gather(cells,starts_with('weights'),key="EventType",value="weight")

#settings
outPath='./Figures/'
proj=c('+proj=gall +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs',
       '+proj=eck4 +lon_0=0 +x_0=0 +y_0=0 +a=6371000 +b=6371000 +units=m +no_defs',
       '+proj=robin +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs',
       '+proj=moll +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs',
       '+proj=aea +lat_1=43 +lat_2=62 +lat_0=30 +lon_0=10 +x_0=0 +y_0=0 +ellps=intl +units=m +no_defs ')
subplotHeight=10.5
subplotWidth=9.9

#layout
layout<-tm_layout(panel.show=FALSE,
                  frame=FALSE,
                  bg.color="#e8f1ff",
                  earth.boundary = TRUE,
                  space.color = 'white',
                  legend.show=FALSE)


#basemap
basemap<-tm_shape(countries,projection =proj[2] )+
  tm_polygons()+
  tm_facets(by="CONTINENT",
          ncol=3,
          free.scales = FALSE,
          showNA=FALSE)+
  tm_shape(countries)+
  tm_polygons()

cellMapList=list()
for(name in names(cells)[3:8]){
  #cell map
  # style
  mapFill<-tm_fill(col=name,
                   style='cont',
                   style.args =list(dataPrecision=0),
                   showNA=FALSE,
                   legend.is.portrait=FALSE,
                   palette = c('#b3abd198','#e66101ff'),  #alpha not working :-( https://github.com/mtennekes/tmap/issues/241
                   alpha=0.8,
                   title="")
  
  cellMapList[[name]]<-basemap+
    tm_shape(cells)+
    mapFill+
    layout
}

facetOfFacets<-tmap_arrange(cellMapList,ncol=2,nrow=3)

#save to files
tmap_save(tm=facetOfFacets,
          filename=paste(outPath,'Figure5.png',sep=''),
          units='cm',
          width=subplotWidth,
          dpi=600)

tmap_save(tm=facetOfFacets,
          filename=paste(outPath,'Figure5.pdf',sep=''),
          units='cm',
          width=subplotWidth,
          dpi=600)

legend<-tm_shape(cells)+
  mapFill+
  tm_layout(legend.only=TRUE)

#save to files
tmap_save(tm=legend,
          filename=paste(outPath,'Figure5_legend.png',sep=''),
          units='cm',
          width=5,
          dpi=600)

tmap_save(tm=legend,
          filename=paste(outPath,'Figure5_legend.pdf',sep=''),
          units='cm',
          width=5,
          dpi=600)



####-------------
#Things tried:

#insets
#europe
euro<-st_bbox(c(xmin = -14.49, xmax = 32.97,
                ymin = 35.23, ymax = 59.26),
              crs = 4326)
euroM<-tm_shape(countries,projection =proj,bbox=euro )+
  tm_polygons()+
  tm_shape(cells)+
  mapFill+
  layoutSmall
print(euroM)
