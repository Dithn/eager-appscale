require 'rubygems'
require 'nokogiri'
require 'open-uri'
require 'cgi'

def get_mashups(count, name, link)
  begin
    mashups_page = Nokogiri::HTML(open("http://www.programmableweb.com#{link}/mashups"))
    m_results_table = mashups_page.css("table[class='listText mB15']")
    m_rows = m_results_table.xpath("//tr")
    puts "#{count} #{name} #{link}"
    m_rows.each do |m_row|
      m_cell = m_row.xpath("td[1]")
      m_name = m_cell.xpath("a/text()")
      if m_name.length > 0
        puts "  --> #{m_name}"
      end
    end
  rescue Exception => ex 
  end
end

count = 1
(1..4).each do |i|
  page = Nokogiri::HTML(open("http://www.programmableweb.com/apis/directory/#{i}"))
  results_table = page.css("table[class='listTable mB15']")
  rows = results_table.xpath("//tr")

  rows.each do |row|
    cell = row.xpath("td[1]")
    name = cell.xpath("a/text()")
    link = cell.xpath("a/@href")
    if name.length > 0
      get_mashups(count, name, link)
      count += 1
      STDOUT.flush
    end
  end
end

