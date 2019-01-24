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