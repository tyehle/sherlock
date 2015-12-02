Sherlock: A Question Answering System
--------
Sherlock, named after IBM Watson's partner, is a question answering system developed by Dasha Pruss and Tobin Yehle as the final project for Natural Language Processing. Sherlock answers questions about current event stories that were collected from the Canadian Broadcasting Corporation (CBC) web page for kids. 

Methods
-------
(This section is under construction.) To identify the sentence in the text that most likely contains the answer, we used a bag-of-words technique to find sentences that had most in common with each question.

To Build
--------
`$ make`


To Run
------
`$ ./run.sh <manifest-file>`


Testing
-------
This was tested on CADE machine `lab1-19.eng.utah.edu`