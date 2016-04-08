floyd_path = function(i, j) {
	if (i == j)
		return [i]
	if (trace_table[i][j] == null)
		return [i, j]

	var c = trace_table[i][j]
	var a = floyd_path(i, c)
	a.pop()
	var b = floyd_path(c, j)

	return a.concat(b);
}

path_for_stations = function(station_a, station_b) {
	var i = station_ids.indexOf(station_a)
	var j = station_ids.indexOf(station_b)
	var path_indexes = floyd_path(i, j)
	var length = path_indexes.length

	path = []
	for (var i = 0; i < length; i++) {
		path.push(station_ids[path_indexes[i]])
	}

	// var remove_duplicates_array = []
	// for ( i = 0; i < path.length; i++ ) {
 //        if (remove_duplicates_array.indexOf(path[i]) < 0) {
 //            remove_duplicates_array.push(path[i]);
 //        }
 //    }
	return path
}

all_stations = function(){
	return stations;
}

floyd_time = function(station_a, station_b) {
	var i = station_ids.indexOf(station_a)
	var j = station_ids.indexOf(station_b)
	return floyd_table[i][j]
}