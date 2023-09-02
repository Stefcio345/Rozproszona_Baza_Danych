===============================WSTĘP===============================
Jest to implementacja rozproszonej bazy danych, projekt na SKJ.
W następnych rozdziałach przedstawiona zostanie infrastruktura bazy, jak korzystać z dołączonych plików,
w jaki sposób moja implementacja bazy przetwarza żądania od klientów oraz jak działa komunikacja wewnątrz bazy danych.

===========================INFRASTRUKTURA===========================
Node'y łączą się ze sobą w dokładnie takiej postaci jak zostaną zainicjowane, 
każdy z nich posiada listę node'ów do których jest podłączony (za pomocą parametru -connect).
W momencie podłącznia nowego noda, node'y do których się podłącza aktualizją listę node'ów
z którymi są połączone o tego nowego noda.

Klasa DatabaseNode posiada pola key i value, w których każdy node przechwouje swój klucz i swoją wartość.

Komunikacja między node'ami odbywa się protokołem TCP na zasadzie serwer-klient,
node A w postaci klienta wysyła żądanie do Noda B, który następnie obsługuje te żądanie i odsyła odpowiedź node'owi A,
któy na nią czeka, aby kontynuuować procesowanie swojego żądania.

Przeszukiwanie sieci w zapytaniach 1-5 odbywa się rekurencyjnie podobnie do serwera DNS.
Podczas przeszukania sieć traktowana jest jak drzewo, którego korzeniem jest node, do którego wysłano żądanie.
Jeżeli node do którego wysłano żądanie nie jest w stanie odpowiedzieć, żądanie przesyłane jest do
kolejnych node'ów z którymi ma połączenie w zmodyfikowanym stanie (z dopisanymi ID już sprawdzonych nodeów, plus jego własnym ID),
żeby uniknąć sprawdzania tych samych node'ów 2 razy.

========================POLECENIA OD KLIENTA========================
Możliwe polecenia, jakie klient może wysłać do serwera w po parametrze -operation <polecenie>.

//set-value <klucz>:<wartość>
Po otrzymaniu żądania od klienta, node sprawdza czy jego klucz zgadza się z poszukiwaną wartością,
jeżeli tak to zmienia wartość (pole value) na dostarczoną wartość i zwaraca komunikat "OK", w przeciwnym wypadku
przesyła to żądanie do wszystkich node z którymi ma połączenie, dodając do niego swoje ID oraz listę dotychczas sprawdzonych node'ów, aby zapobiec ponownemu sprawdzaniu
tego samego node 2 razy. Następnie oczekuje na odpowiedzi od nich, jeżeli zwrócą ERROR również go zwraca,
w przeciwnym wypadku, zwraca komunikat "OK".

//get-value <klucz>
Po otrzymaniu żądania od klienta, node sprawdza czy jego klucz zgadza się z posuzkiwaną wartością,
jeżeli tak to zwraca swoją wartość (pole value), w przeciwnym wypadku
przesyła to żądanie do wszystkich node z którymi ma połączenie, dodając do niego swoje ID oraz listę dotychczas sprawdzonych node'ów, aby zapobiec ponownemu sprawdzaniu
tego samego node 2 razy. Następnie oczekuje na odpowiedzi od nich, jeżeli zwrócą ERROR również go zwraca,
w przeciwnym wypadku, zwraca wartość (pole value) od node w którym klucz się zgadza z poszukiwaną warością.

//find-key <klucz>
Po otrzymaniu żądania od klienta, node sprawdza czy jego klucz zgadza się z posuzkiwaną wartością,
jeżeli tak to zwraca wartość w postaci <swój adres>:<swój port>, w przeciwnym wypadku
przesyła to żądanie do wszystkich node z którymi ma połączenie, dodając do niego swoje ID oraz listę dotychczas sprawdzonych node'ów, aby zapobiec ponownemu sprawdzaniu
tego samego node 2 razy. Następnie oczekuje na odpowiedzi od nich, jeżeli zwrócą ERROR również go zwraca,
w przeciwnym wypadku, zwraca otrzymaną od połączonego node wartość w postaci <adres>:<port> noda na którym znajduje się szukany klucz.

//get-max
Wysyła żądania połączonych node'ów, które wysyłają to samo żądanie do swoich połączonych node'ów,
aż żądania dojdą do gałęzi drzewa, wtedy rekurencyjnie wracają do noda rzędem wyżej zwarcając swoje wartości,
które następnie są porównywane w wyższym node, który zwraca największą z nich/bądź swoją wartość jeśli jest największa.

//get-min
Działa na dokładnie takiej samej zasadzie co GET-MAX, tylko liczby porównuje się w odwrotny sposób.

//new-record <klucz>:<wartość>
klasa node nadpisuje podanymi wartościami swoje pola key i value, następnie zwraca OK

//terminate
Node wysyła do wszystkich node'ów z którymi jest połączony żądanie o usunięcie go z ich listy
połączonych node'ów, a następnie odsyła klientowi wiadomość OK, kończy pętle
odpowiedzialną za obsługę żądań i zamyka serverSocket. 

==========================JAK ZAINSTALOWAĆ==========================
Do działania niezbędne jest JDK 1.8+
W pliku .zip znajdują się 2 pliki .java, które zostały już skompilowane, 
w razie wprowadzania w nich zmian, należy uruchomić skrypt compile.bat, który na nowo je skompiluje.


DatabaseClient.java, który pozwala wysyłać żądania do bazy danych, argumenty wywołania:
[-gateway <adres ip>:<numer port> określa adres ip i numer portu node do którego klient ma się podłączyć
- operation <żądanie> określa żądanie, które klient ma wysłać do noda (Możliwe operacje są wyjaśnione w rozdziale wyżej)]

wywołanie klienta ma następującą postać: 
java DatabaseClient -gateway <adres node do którego chcemy się połączyć>:<port na którym działa ten node> -operation <polecenie dla serwera>


Oraz DatabaseNode.java, serwer bazy danych, argumenty wywołania: 
[-tcpport <numer portu TCP> określa numer portu TCP na którym dany węzeł sieci
oczekuje na połączenia od klientów.
-record <klucz>:<wartość> oznacza parę liczb całkowitych początkowo
przechowywanych w bazie na danym węźle, gdzie pierwsza to klucz a druga to wartość
związana z tym kluczem. Nie ma wymogu unikalności zarówno klucza jak i wartości.
[ -connect <adres>:<port> ] oznacza listę innych węzłów już będących w sieci, z
którymi dany węzeł ma się połączyć i z którymi może się komunikować w celu wykonywania
operacji. Lista może zawierać wiele węzłów. Dla pierwszego węzła w sieci ta lista jest pusta.]

wywołanie node ma następującą postać: 
java DatabaseNode -tcpport <numer portu na którym node będzie nasłuchiwał> -record <klucz przechowywany na node>:<wartość przechowywana na node> [ -connect <adres node, z którym nasz node ma być połączony>:<port na którym słucha ten node> ]

====================CO ZOSTAŁO ZAIMPLEMENTOWANE=====================
Zaimplementowana zotała obsługa wszystkich poleceń od klienta, łączenie się Node'ów w kompletną sieć
oraz komunikacja między node'ami potrzebna do realizacji poleceń od klienta.

==================CO NIE ZOSTAŁO ZAIMPLEMENTOWANE===================
Nie została zaimplementowana funkcjonalność pozwalająca na korzystanie 
kilku klientów z bazy danych w tym samym czasie.

Na bazie działają wszystkie testowe skrypty poza "script-7-p.bat".


-Franciszek Kępski, s25925
