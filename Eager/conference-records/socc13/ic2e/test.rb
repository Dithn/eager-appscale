def do_stuff
  yield(25)
end

do_stuff { |value| puts value }
do_stuff { |value| puts 10 + value } 
