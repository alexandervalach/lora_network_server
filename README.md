# LoRa Network Server

Project aimed at creating open-source server which could process LoRaWAN and LoRa@FIIT messages.
This is only edited version, originally created by Karol Cagáň and Ondrej Perešíni.

## Research

This project has been further enhanced or altered by several research papers:
- A. Valach and D. Macko, "Upper Confidence Bound Based Communication Parameters Selection to Improve Scalability of LoRa@FIIT Communication," in IEEE Sensors Journal, vol. 22, no. 12, pp. 12415-12427, 15 June15, 2022, doi: [10.1109/JSEN.2022.3174663](https://doi.org/10.1109/JSEN.2022.3174663).
- A. Valach and D. Macko, "Optimization of LoRa Devices Communication for Applications in Healthcare," 2020 43rd International Conference on Telecommunications and Signal Processing (TSP), Milan, Italy, 2020, pp. 511-514, doi: [10.1109/TSP49548.2020.9163432](https://doi.org/10.1109/TSP49548.2020.9163432).

Feel free to open any issues / suggestions in the form of pull requests. Comments in the form, like you should do this are meaningless, as if I had another lifetime I would have certainly fixed it. Just make yourself useful.~~

## Steps to run the application

1. Download the source code.
2. Change the desired values in resources / configuration.config (copy the original cofiguration and rename it if neccessary).
3. Open the project in IntelliJ IDEA to Build artifacts or compile java code and create 
4. Upload the compiled Java file to service location or create a CI / CD pipeline as a pull request.

### Install the application as a system service, onlyLinux
Run the script bin / install_service.sh as a user with root privileges.
In case of any troubles, open pull request