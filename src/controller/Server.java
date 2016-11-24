package controller;

import model.*;
import java.io.*;
import java.util.*;

import javax.swing.text.AbstractDocument.LeafElement;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class Server implements Observer
{
	private static Server data = new Server();
	
	private List<Player> players = new ArrayList<Player>();
	
	public List<Socket> clients = new ArrayList<Socket>();
	
	public String[] teams = {"Azul","Vermelho", "Verde", "Amarelo"};
	
	public ServerSocket server;
	
	public Server() 
	{		
		
	}
	
	public static Server sharedInstance()
	{
		return data;
	}
	
	public void connect(int porta)
	{
		try {
			this.server = new ServerSocket(porta);
			System.out.println("ServerSocket criado");
			
			//Aguarda pela conexao dos clientes
			this.waitClients();
	
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void waitClients()
	{
		new Thread() {
			
			@Override
			public void run()
			{
				while(players.size() <= 4)
				{
					try {
						Socket client =  server.accept();
						clients.add(client);
						
						System.out.println("Cliente entrou");
						
						MessageHandler handler = new MessageHandler(client);
						
						Thread thread = new Thread(handler);
						
						thread.start();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				if(players.size() == 4)
				{
					return;
				}
			}
		}.start();
	}
	
	
	public void disconnect()
	{
		try {
			this.server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Boolean sendMessage(Socket client, byte[] bytes)
	{
		try {
			
			OutputStream output = client.getOutputStream();
			
			output.write(bytes, 0, bytes.length);			
			output.flush();
			
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		// TODO Auto-generated method stub
		
		MessageHandler handler = (MessageHandler) o;
		
		HashMap<String, Object> map = (HashMap<String, Object>) arg;
		
		//AUTENTICACAO
		if (map.containsKey("nickname"))
		{
			String currentTeam = this.teams[0];;
			
			//Encontra o time correto
			for (int i = 0; i < this.players.size(); i++)
			{
				if (currentTeam == this.players.get(i).team)
				{
					if(i < this.teams.length - 1)
					{
						currentTeam = this.teams[i+1];
					}
					else
					{
						return;
					}
				}
			}
			
			Player player = new Player(handler.client, (String) map.get("nickname"), currentTeam);
			this.players.add(player);
			
			String message = "Cliente " + player.nickname + " conectou-se com o time " + currentTeam;  
			
			System.out.println(message);		
			
			
			//Encontra o cliente que efetuou a autenticacao
			this.clients.remove(handler.client);
			
			map.put("team", currentTeam);
			
			String content = map.toString() + "\n";
						
			byte[] bytes = content.getBytes();
			
			this.sendToAllPlayers(bytes);
			
			//enviar para jogador que logou os abiguinhos conectados
			this.sendOpponents(player);
		}
		//Outros eventos
		else
		{
			String content = map.toString() + "\n";
			
			byte[] bytes = content.getBytes();
						
			this.sendToAllPlayers(bytes);

		}
	}
	
	//envia para jogador que logou seus oponentes já conectados
	void sendOpponents(Player player)
	{
		Map<String, Object> map = new HashMap<String, Object>();
		
		for(int i=0; i<this.players.size(); i++)
		{
			if(!this.players.get(i).nickname.equals(player.nickname));
			{
				map.put("nickname", this.players.get(i).nickname);
				map.put("team", this.players.get(i).team);
				String content = map.toString() + "\n";
				byte[] bytes = content.getBytes();
				this.sendMessage(player.socket, bytes);
			}	
		}		
	}
	
	void sendToAllPlayers(byte[] bytes)
	{
		System.out.println("Reenviando mensagem para clientes. Conteudo:");
		
		String content = new String(bytes, StandardCharsets.UTF_8);
				
		//Reenvia a mensagem para todos os clientes
		for (Player player : this.players)
		{	
			this.sendMessage(player.socket, bytes);
		}
	}
	
	void printLoggedClients()
	{
		System.out.println("Clientes online:");

		for (Player player : players) 
		{
			System.out.println(player.nickname);
		}
	}
}
