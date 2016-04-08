#!/usr/bin/ruby
require 'spreadsheet'
require 'roo'
require 'json'

book = Roo::Spreadsheet.open("./subway_data.xlsx")

stations_by_internal_id = Hash.new
station_sheets = []
path_sheets = []
(0...(book.sheets.count)).each do |i|
	if book.sheets[i] == "구간정보"
		path_sheets.push(i)
	else
		station_sheets.push(i)
	end
end

# 엑셀 데이터를 로드한다.
station_sheets.each do |i|
	sheet = book.sheet(i)

	header_row = book.first_row
	headers = book.row(header_row)

	column_indexes = {
		internal_id: headers.find_index("고유번호"),
		id: headers.find_index("역 ID"),
		line: headers.find_index("노선번호"),
		name_ko: headers.find_index("역명(한글)"),
		name_en: headers.find_index("역명(영어)"),
		name_cn: headers.find_index("역명(중국어)"),
		location: headers.find_index("역위치 (위,경도)"),
		exit_locations: headers.find_index("출구위치 (위,경도)"),
		transfers: headers.find_index("환승가능역(고유번호)")
	}

	def to_string(a)
		if a.is_a?(String)
			return a
		elsif a.is_a?(Integer)
			return a.to_s
		elsif a.is_a?(Float)
			return a.to_i.to_s
		end
		""
	end
	((sheet.first_row + 1)...(sheet.last_row + 1)).each do |j|
		row = sheet.row(j)

		internal_id = to_string(row[column_indexes[:internal_id]])
		id = to_string(row[column_indexes[:id]])
		line = to_string(row[column_indexes[:line]])
		name_ko = to_string(row[column_indexes[:name_ko]])
		name_en = to_string(row[column_indexes[:name_en]])
		name_cn = to_string(row[column_indexes[:name_cn]])
		(lat,lon) = to_string(row[column_indexes[:location]]).split(",").map{|s|s.strip}
		exits = to_string(row[column_indexes[:exit_locations]]).lines.map do |l|
			(number, lat, lon) = l.sub(/\((.*)\)/, '\1').split(",").map{|s|s.strip}
			{
				number: number,
				latitude: lat,
				longitude: lon
			}
		end
		transfers = to_string(row[column_indexes[:transfers]]).split(",").map{|s|s.strip}
		stations_by_internal_id[internal_id] = {
			id: id,
			line: line,
			name_ko: name_ko,
			name_en: name_en,
			name_cn: name_cn,
			location: {
				latitude: lat,
				longitude: lon
			},
			exits: exits,
			transfers: transfers
		}
	end
end

# internal_id 기준이 아닌 id 기준으로 데이터를 구성한다.
stations = Hash.new
transfer_stations = Array.new
stations_by_internal_id.each do |k, v|
	stations[v[:id]] = {
		id: v[:id],
		line: v[:line],

		name_ko: v[:name_ko],
		name_en: v[:name_en],
		name_cn: v[:name_cn],
		location: v[:location],
		exits: Hash[
			v[:exits].map {|e|
				[
					e[:number],
					{
						latitude: e[:latitude],
						longitude: e[:longitude],
					}
				]
			}
		],
		lines: Array.new,
		isDuplicate: false,
		# transfers: v[:transfers].map{|s|stations_by_internal_id[s][:id]},
		# 환승은 5분으로 통일한다.
		# connections: Hash[
		# 	v[:transfers].map {|s|
		# 		[stations_by_internal_id[s][:id], 5]
		# 	}
		# ]
		connections: Hash.new
	}

	if v[:transfers].count > 0
		transfer_station = []
		transfer_station.push(v[:id])
		transfer_station.concat(v[:transfers].map{|s|stations_by_internal_id[s][:id]})

		t_id = transfer_station.join

		transfer_station_lines = []
		transfer_station_lines.push(v[:line])
		transfer_station_lines.push(v[:transfers].map{|s|stations_by_internal_id[s][:line]})


		isContain = false
		transfer_id = "";
		for i in 0..(transfer_stations.count - 1)
			if transfer_stations[i].include? v[:id]
				transfer_id = transfer_stations[i]
				isContain = true
			end
		end


		if stations[t_id] == nil
			if !isContain
				transfer_stations.push(t_id)
				stations[t_id] = {
					id: t_id,
					lines: transfer_station_lines.flatten.each {|s|s},
					line: "환승역",
					isDuplicate: false,
					name_ko: v[:name_ko],
					name_en: v[:name_en],
					name_cn: v[:name_cn],
					location: v[:location],
					exits: stations[v[:id]][:exits],
					connections: Hash[transfer_station.map{|s|[s, 2]}]
				}
			else
				stations[t_id] = {
					id: transfer_id,
					lines: transfer_station_lines.flatten.each {|s|s},
					line: "환승역",
					isDuplicate: true,
					name_ko: v[:name_ko],
					name_en: v[:name_en],
					name_cn: v[:name_cn],
					location: v[:location],
					exits: stations[v[:id]][:exits],
					connections: Hash[transfer_station.map{|s|[s, 2]}]
				}
			end
			stations[v[:id]][:connections][t_id] = 1
		end


	end
end

# 인접한 역과 소요시간 정보를 산출한다.
path_sheets.each do |i|
	sheet = book.sheet(i)

	header_row = book.first_row
	headers = book.row(header_row)

	column_indexes = {
		line_name: headers.find_index("호선번호"),
		start_id: headers.find_index("startID"),
		end_id: headers.find_index("endID"),
		time: headers.find_index("소요시간(분)"),
		directed: headers.find_index("단방향여부")
	}

	((sheet.first_row + 1)...(sheet.last_row + 1)).each do |j|
		row = sheet.row(j)
		h = {
			line_name: to_string(row[column_indexes[:line_name]]),
			start_id: to_string(row[column_indexes[:start_id]]),
			end_id: to_string(row[column_indexes[:end_id]]),
			time: row[column_indexes[:time]].to_i,
			directed: to_string(row[column_indexes[:directed]])
		}

		s = stations[h[:start_id]]
		s[:connections][h[:end_id]] = h[:time]
		stations[h[:start_id]] = s

		if h[:directed] != "단방향"
			s = stations[h[:end_id]]
			s[:connections][h[:start_id]] = h[:time]
			stations[h[:end_id]] = s
		end
	end
end

station_ids = stations.keys
floyd_table = Array.new(station_ids.count) { Array.new(station_ids.count) }
trace_table = Array.new(station_ids.count) { Array.new(station_ids.count) }

for i in 0..(station_ids.count - 1)
	for j in 0..(station_ids.count - 1)
		if i == j then
			floyd_table[i][j] = 0
			trace_table[i][j] = nil
		else
			id_i = station_ids[i]
			id_j = station_ids[j]
			floyd_table[i][j] = stations[id_i][:connections][id_j]
			if floyd_table[i][j] == nil then
				trace_table[i][j] = i
			else
				trace_table[i][j] = nil
			end
		end
	end
end

for k in 0..(station_ids.count - 1)
	for i in 0..(station_ids.count - 1)
		for j in 0..(station_ids.count - 1)
			if not floyd_table[i][k].nil? and not floyd_table[k][j].nil?
				newLength = floyd_table[i][k] + floyd_table[k][j]
				if floyd_table[i][j].nil? or floyd_table[i][j] > newLength
					floyd_table[i][j] = newLength
					trace_table[i][j] = k
				end
			end
		end
	end
end

puts "stations = " + JSON.generate(stations)
puts "station_ids = " + JSON.generate(station_ids)
puts "floyd_table = " + JSON.generate(floyd_table)
puts "trace_table = " + JSON.generate(trace_table)
