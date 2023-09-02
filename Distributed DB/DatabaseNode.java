import java.net.*;
import java.io.*;
import java.util.*;

public class DatabaseNode {

    static int value;
    static int key;
    static int listenPort;

    public static void main(String[] args) throws IOException, InterruptedException {

        LinkedList<String> adresses = new LinkedList<>();
        LinkedList<Node> nodes = new LinkedList<>();
        String query;
        String[] temp;
        String responseFromNode;
        boolean endFlag = false;

        //Przetworzenie parametrów
        for (int i=0; i<args.length; i++) {
            switch (args[i]) {
                case "-tcpport":
                    listenPort = Integer.parseInt(args[++i]);
                    break;
                case "-record":
                    temp = args[++i].split(":");
                    key = Integer.parseInt(temp[0]);
                    value = Integer.parseInt(temp[1]);
                    break;
                case "-connect":
                    adresses.add(args[++i]);
                    break;
                default:
                    System.out.println("No such argument as: " + args[i]);
            }
        }

        String MyAdresPort = Inet4Address.getLocalHost().getHostAddress() + ":" + listenPort;

        //Connect to all listed nodes
        for (String adress : adresses){
            temp = adress.split(":");
            //Add all nodes to a list
            if(Objects.equals(temp[0], "localhost")) temp[0] = Inet4Address.getLocalHost().getHostAddress();
            nodes.add(connectToNode(temp[0], Integer.parseInt(temp[1])));
        }

        ServerSocket serverSocket = new ServerSocket(listenPort);

        while(!endFlag){
            boolean gotAnswer = false;
            boolean success = false;
            //Connect with client/other node and read query
            System.out.println("Waiting for connection...");
            Socket socket = serverSocket.accept();
            System.out.println("Connected to " + socket.getInetAddress().toString() + ":" + socket.getPort());
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            System.out.println("Reading message...");

            query = br.readLine();
            System.out.println("Received query from client: " + query + "\nProcessing...");
            temp = query.split(" ");


            PrintWriter responseToClient = new PrintWriter(socket.getOutputStream(), true);

            //Remove checked nodes
            LinkedList<Node> checkedNodes = new LinkedList<>();
            if (temp.length > 1){
                if (temp[1].length() > 12) {
                    for (int i = 1; i < temp.length; i++) {
                        checkedNodes.add(new Node(temp[i]));
                    }
                } else if (temp.length > 2) {
                    if(temp[2].length() > 12) {
                        for (int i = 2; i < temp.length; i++) {
                            checkedNodes.add(new Node(temp[i]));
                        }
                    }
                }
            }

            //Add all connected nodes that are not yet checked
            LinkedList<Node> nodesToCheck = new LinkedList<>();
            boolean checkedFlag = false;
            for(Node node : nodes){
                checkedFlag = false;
                for(Node checkNode : checkedNodes){
                    if (checkNode.adressPort.equals(node.adressPort)) {
                        checkedFlag = true;
                        break;
                    }
                }
                if(!checkedFlag) nodesToCheck.add(node);
            }

            /**System.out.print("Checked nodes: [");
            for(Node n : checkedNodes){
                System.out.print(n.adressPort + ", ");
            }
            System.out.print("]");

            System.out.print("Nodes to be checked: [");
            for(Node n : nodesToCheck){
                System.out.print(n.adressPort + ", ");
            }
            System.out.print("]");*/

            //Process query from client
            switch (temp[0]) {

                case "set-value":
                    temp = temp[1].split(":");
                    //If no more nodes to check return check and return ERROR or value
                    if(nodesToCheck.isEmpty() || key == Integer.parseInt(temp[0])){
                        if(key == Integer.parseInt(temp[0])) {
                            value = Integer.parseInt(temp[1]);
                            responseToClient.println("OK");
                            responseToClient.close();
                        }else{
                            responseToClient.println("ERROR");
                            responseToClient.close();
                        }
                    }
                    //Else ask neighbouring nodes for their values and check if they are correct
                    else{
                        for(Node node: nodesToCheck){
                            //Turn checked nodes into string
                            String checkedNodesStr = "";
                            for(Node checNode : checkedNodes){
                                checkedNodesStr += " " + checNode.adressPort;
                            }
                            //Send query to every connected node, and compare values
                            responseFromNode = node.writeReadClose("set-value " + temp[0] + ":" + temp[1] + " " + MyAdresPort + checkedNodesStr);
                            if(responseFromNode.equals("ERROR")){
                                //If error, keep searching
                                continue;
                            }else{
                                //Else stop searching and send answer
                                gotAnswer = true;
                                responseToClient.println(responseFromNode);
                                responseToClient.close();
                                break;
                            }
                        }
                        if (!gotAnswer) {
                            responseToClient.println("ERROR");
                            responseToClient.close();
                        }
                    }
                    break;


                case "get-value":
                    //If no more nodes to check return check and return ERROR or value
                    if(nodesToCheck.isEmpty() || key == Integer.parseInt(temp[1])){
                        if(key == Integer.parseInt(temp[1])) {
                            responseToClient.println(key + ":" + value);
                            responseToClient.close();
                        }else{
                            responseToClient.println("ERROR");
                            responseToClient.close();
                        }
                    }
                    //Else ask neighbouring nodes for their values and check if they are correct
                    else{
                        for(Node node: nodesToCheck){
                            //Turn checked nodes into string
                            String checkedNodesStr = "";
                            for(Node checNode : checkedNodes){
                                checkedNodesStr += " " + checNode.adressPort;
                            }
                            //Send query to every connected node, and compare values
                            responseFromNode = node.writeReadClose("get-value " + temp[1] + " " + MyAdresPort + checkedNodesStr);
                            if(responseFromNode.equals("ERROR")){
                                //If error, keep searching
                                continue;
                            }else{
                                //Else stop searching and send answer
                                gotAnswer = true;
                                responseToClient.println(responseFromNode);
                                responseToClient.close();
                                break;
                            }
                        }
                        if (!gotAnswer) {
                            responseToClient.println("ERROR");
                            responseToClient.close();
                        }
                    }
                    break;


                case "find-key":
                    //If no more nodes to check return check and return ERROR or value
                    if(nodesToCheck.isEmpty() || key == Integer.parseInt(temp[1])){
                        if(key == Integer.parseInt(temp[1])) {
                            responseToClient.println(MyAdresPort);
                            responseToClient.close();
                        }else{
                            responseToClient.println("ERROR");
                            responseToClient.close();
                        }
                    }
                    //Else ask neighbouring nodes for their values and check if they are correct
                    else{
                        for(Node node: nodesToCheck){
                            //Turn checked nodes into string
                            String checkedNodesStr = "";
                            for(Node checNode : checkedNodes){
                                checkedNodesStr += " " + checNode.adressPort;
                            }
                            //Send query to every connected node, and compare values
                            responseFromNode = node.writeReadClose("find-key " + temp[1] + " " + MyAdresPort + checkedNodesStr);
                            if(responseFromNode.equals("ERROR")){
                                //If error, keep searching
                                continue;
                            }else{
                                //Else stop searching and send answer
                                gotAnswer = true;
                                responseToClient.println(responseFromNode);
                                responseToClient.close();
                                break;
                            }
                        }
                        if (!gotAnswer) {
                            responseToClient.println("ERROR");
                            responseToClient.close();
                        }
                    }
                    break;


                case "get-max":
                    int maxValue = value;
                    int maxKey = key;
                    //If no more nodes to check return max value
                    if(nodesToCheck.isEmpty()){
                        responseToClient.println(maxKey + ":" + maxValue);
                        responseToClient.close();
                    }
                    //Else ask neighbouring nodes for their values and check if they are higher than this one
                    else{
                        for(Node node: nodesToCheck){
                            //Turn checked nodes into string
                            String checkedNodesStr = "";
                            for(Node checNode : checkedNodes){
                                checkedNodesStr += " " + checNode.adressPort;
                            }
                            //Send query to every connected node, and compare values
                            responseFromNode = node.writeReadClose("get-max " + MyAdresPort + checkedNodesStr);
                            System.out.println(responseFromNode);
                            String[] response = responseFromNode.split(":");
                            if(Integer.parseInt(response[1]) > maxValue){
                                //If neighbouring node has higher value, change max value
                                maxKey = Integer.parseInt(response[0]);
                                maxValue = Integer.parseInt(response[1]);
                            }
                        }
                        //Sent max value to client
                        responseToClient.println(maxKey + ":" + maxValue);
                        responseToClient.close();
                    }
                    break;


                case "get-min":
                    int minValue = value;
                    int minKey = key;
                    //If no more nodes to check return min value
                    if(nodesToCheck.isEmpty()){
                        System.out.println("Test");
                        responseToClient.println(minKey + ":" + minValue);
                        responseToClient.close();
                    }
                    //Else ask neighbouring nodes for their values and check if they are lower than this one
                    else{
                        for(Node node: nodesToCheck){
                            //Turn checked nodes into string
                            String checkedNodesStr = "";
                            for(Node checNode : checkedNodes){
                                checkedNodesStr += " " + checNode.adressPort;
                            }
                            //Send query to every connected node, and compare values
                            responseFromNode = node.writeReadClose("get-min " + MyAdresPort + checkedNodesStr);
                            System.out.println(responseFromNode);
                            String[] response = responseFromNode.split(":");
                            if(Integer.parseInt(response[1]) < minValue){
                                //If neighbouring node has higher value, change max value
                                minKey = Integer.parseInt(response[0]);
                                minValue = Integer.parseInt(response[1]);
                            }
                        }
                        //Sent max value to client
                        responseToClient.println(minKey + ":" + minValue);
                        responseToClient.close();
                    }
                    break;


                case "new-record":
                    temp = temp[1].split(":");
                    //Set new values
                    key = Integer.parseInt(temp[0]);
                    value = Integer.parseInt(temp[1]);
                    //Print new values
                    System.out.println("New key: " + key);
                    System.out.println("New value: " + value);
                    //Send OK to client
                    responseToClient.println("OK");
                    responseToClient.close();
                    break;
                case "terminate":
                    //Iterate through connected nodes
                    for(Node node: nodes){
                        //Send delete-node signal to all of them
                        node.writeReadClose("delete-node " + MyAdresPort);
                    }
                    responseToClient.println("OK");
                    responseToClient.close();
                    //end loop
                    endFlag = true;
                    break;
                case "delete-node":
                    for(int i = 0; i < nodes.size(); i++){
                        if(nodes.get(i).adressPort.equals(temp[1])){
                            System.out.println("Removed node " + nodes.get(i).adressPort + " from chain");
                            nodes.remove(nodes.get(i));
                        }
                    }
                    System.out.println("Current list of nodes: ");
                    for (Node node : nodes){
                        System.out.println(node.adressPort);
                    }
                    responseToClient.println("OK");
                    responseToClient.close();
                    break;
                case "add-node":
                    System.out.println("Adding new node to chain");
                    temp = temp[1].split(":");
                    nodes.add(new Node(temp[0], Integer.parseInt(temp[1])));
                    System.out.println("Current list of nodes: ");
                    for (Node node : nodes){
                        System.out.println(node.adressPort);
                    }
                    responseToClient.println("OK");
                    responseToClient.close();
                    break;
                default:
                    break;
            }
            System.out.println("Finished processing\n");
        }
        serverSocket.close();
    }

    public static Node connectToNode(String adress, int port) throws IOException {
        Node node = new Node(adress, port);
        node.Initialize();
        return node;
    }

    static class Node{
        String adressPort;
        String adress;
        int port;

        Node(String adress, int port){
            this.adress = adress;
            this.port = port;
            adressPort = adress + ":" + Integer.toString(port);
        }

        Node(String adressPort){
            String[] temp = adressPort.split(":");
            this.adress = temp[0];
            this.port = Integer.parseInt(temp[1]);
            this.adressPort = adressPort;
        }

        void Initialize() throws IOException {
            writeToClient("add-node " + Inet4Address.getLocalHost().getHostAddress() + ":" + listenPort);
        }

        String writeReadClose(String str) throws IOException {
            Socket socket = new Socket(adress, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(str);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String response = in.readLine();
            socket.close();
            return response;
        }

        void writeToClient(String str) throws IOException {
            Socket socket = new Socket(adress, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(str);
            out.close();
        }
    }

}