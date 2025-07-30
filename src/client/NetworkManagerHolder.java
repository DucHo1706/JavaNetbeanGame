/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

/**
 *
 * @author duc18
 */
public class NetworkManagerHolder {
     private static NetworkManager networkManager;

    public static void setNetworkManager(NetworkManager nm) {
        networkManager = nm;
    }

    public static void sendMessage(String message) {
        if (networkManager != null && networkManager.isConnected()) {
            networkManager.sendMessage(message);
        } else {
            System.err.println("NetworkManager chưa được khởi tạo hoặc chưa kết nối!");
        }
    }
}
