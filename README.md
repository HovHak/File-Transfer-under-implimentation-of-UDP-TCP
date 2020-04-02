### File-Transfer under implimentation of UDP&TCP 

-------------

### To run

-------------
First you will have to use netbeans or any IDE that will alow you to call and change the arguments of the main method, please follow example on netbeans below.

After opening the project for UDPServer/Client or TCPServer/Client check the customize option in both UDPClient and TCPClient.

----

<p align="center"><img src = "images/source.PNG" ></p>


Click on the Customize method and you will see a new window where you can pass all of your arguments to the client. Example Below


<p align="center"><img src = "images/interface.PNG" ></p>

### Note
----
* Both client and the server can be used on the same source, however if you decide to try and run one of them on different computer one as a server and the other as client, you would then need to change the variable that is called IP to the one that is used on your local computer.


* At the section where it says Arguments, you are given a line where you can type your first argument which is going to be what you what you intend to do  e.g. send or recieve a file which can be defined as ‘read’ or ‘write’ 

* After you decided to use either w'write' or 'read' you can press space and type in the second argument which is going to be the name of the file with its type. 

* Both UDP and TCP can be run with the same way.

* You can connect as many clients a syou want

#### Important

* Run the server first then only run the client such that the client is able to connect with the server that is already running
* Make sure to have a space between two arguments otherwise it will understand it as a one argument. 
* Make sure that argument should be either ‘read’ or ‘write’ with no capitals. 
