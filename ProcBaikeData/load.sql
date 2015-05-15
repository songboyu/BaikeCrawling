use baidubaike2015;

load data local infile 'baike_data_final/tags.txt'
into table tags 
fields terminated by '\t' 
lines TERMINATED BY '\n' ;

load data local infile "baike_data_final/entries.txt"
into table entries
fields terminated by '\t' 
lines TERMINATED BY '\n' ;

load data local infile "baike_data_final/relations.txt"
into table relations
fields terminated by '\t' 
lines TERMINATED BY '\n' ;

load data local infile "baike_data_final/inlinks.txt"
into table inlinks
fields terminated by '\t' 
lines TERMINATED BY '\n' ;