all: paper-short

paper:  
	pdflatex paper
	bibtex paper
	pdflatex paper
	pdflatex paper

paper-short:  
	pdflatex paper-short
	bibtex paper-short
	pdflatex paper-short
	pdflatex paper-short
clean: 
	rm -f paper.aux paper.bbl paper.blg paper.dvi paper.log paper-short.aux paper-short.bbl paper-short.blg paper-short.dvi paper-short.log
