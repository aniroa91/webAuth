function getErrorHourlyHeatmap(data){
    var jsonArr = [];
    for (var i =0 ; i < data.length; i++) {
        for(var j=0;j<7;j++){
            //if(Number(data[i][j])>0)
            jsonArr.push([Number(data[i][0]),j,Number(data[i][j+1])]);
        }
    }
    return jsonArr
}

// right click event
function fntRightClick(){
    (function (Highcharts) {

        Highcharts.wrap(Highcharts.Chart.prototype, 'firstRender', function (proceed) {

            proceed.call(this);

            var chart = this,
                container = this.container,
                plotLeft = this.plotLeft,
                plotTop = this.plotTop,
                plotWidth = this.plotWidth,
                plotHeight = this.plotHeight,
                inverted = this.inverted,
                pointer = this.pointer;

            // Note:
            // - Safari 5, IE8: mousedown, contextmenu, click
            // - Firefox 5: mousedown contextmenu
            container.oncontextmenu = function(e) {

                var hoverPoint = chart.hoverPoint,
                    chartPosition = pointer.chartPosition;

                this.rightClick = true;

                e = pointer.normalize(e);

                e.cancelBubble = true; // IE specific
                e.returnValue = false; // IE 8 specific
                if (e.stopPropagation) {
                    e.stopPropagation();
                }
                if (e.preventDefault) {
                    e.preventDefault();
                }

                if (!pointer.hasDragged) {
                    if (hoverPoint && pointer.inClass(e.target, 'highcharts-tracker')) {
                        var plotX = hoverPoint.plotX,
                            plotY = hoverPoint.plotY;

                        // add page position info
                        Highcharts.extend(hoverPoint, {
                            pageX: chartPosition.left + plotLeft +
                            (inverted ? plotWidth - plotY : plotX),
                            pageY: chartPosition.top + plotTop +
                            (inverted ? plotHeight - plotX : plotY)
                        });

                        // the point click event
                        hoverPoint.firePointEvent('contextmenu', e);
                    }
                }
            }
        });

    }(Highcharts));
}

function getDrildownForCol(dta, arrKey){
    var jsArray = []
    var arrData = []
    for(var key=0;key<arrKey.length;key++ ) {
        var sumVl =0
        for (var i = 0; i < dta.length; i++) {
            if (arrKey[key] == dta[i][0]){
                sumVl += dta[i][3]
            }
        }
        arrData.push({
            name: arrKey[key],
            y: sumVl,
            drilldown: arrKey[key]
        })
    }
    jsArray.push({
        name: 'Province',
        data: arrData,
        color: '#832d1d'
    })
    return jsArray
}

function getDrillDown2LevelForCol(dta, arrRegion){
    var jsArray = []
    for(var region=0;region< arrRegion.length;region++ ){
        // get array provinces and bras by month&region
        var arrProvinceBras = [], dtaRegion = []
        for(var i=0;i< dta.length;i++){
            if (arrRegion[region] == dta[i][0]){
                arrProvinceBras.push({
                    province:dta[i][1],
                    bras: dta[i][2],
                    value:dta[i][3]
                })
            }
        }
        // set array data by province
        var nameOld = arrProvinceBras[0].province, sumOld= 0,
            isMoreProv =0,dtaProvince = []
        for(var k=0;k<arrProvinceBras.length;k++){
            if(k==(arrProvinceBras.length -1) || nameOld != arrProvinceBras[k].province){
                if(k==(arrProvinceBras.length -1)) sumOld += arrProvinceBras[k].value
                dtaRegion.push({
                    name: nameOld,
                    y: sumOld,
                    drilldown:nameOld
                })
                // set series province& bras
                jsArray.push({
                    id: nameOld,
                    name: nameOld,
                    data: dtaProvince
                })
                dtaProvince = []
                nameOld = arrProvinceBras[k].province
                sumOld = arrProvinceBras[k].value
                isMoreProv =1
            }
            else{
                sumOld += Number(arrProvinceBras[k].value)
            }
            dtaProvince.push({
                name: arrProvinceBras[k].bras,
                y:Number(arrProvinceBras[k].value)
            })
        }
        if(isMoreProv ==0){
            dtaRegion.push({
                name: nameOld,
                y: Number(sumOld),
                drilldown:nameOld
            })
            // set series province& bras
            jsArray.push({
                id: nameOld,
                name: nameOld,
                data: dtaProvince
            })
        }

        // set series regions& province
        jsArray.push({
            id: arrRegion[region],
            name: arrRegion[region],
            data: dtaRegion
        })
    }
    return jsArray
}