/**Game
 * @author Bradley Johns
 * An object class containing the basic information
 * required for a game in the app
 */

package com.example.digitalDeck;

public class Game { //Note: MAKE ME ABSTRACT
    
    private int size;
    private int numPlayers;
    private String[] players;
    private String title;

    /**Game constructor
     * Construct a game given a number of players,
     * the creating host, and a title
     * @param gameSize the number of players in the game
     * @param gameHost the host of the game
     * @param gameTitle the title of the game
     */

    public Game(int gameSize, String gameHost, String gameTitle) {
        size = gameSize;
        numPlayers = 1;
        players = new String[size];
        players[0] = gameHost;
        title = gameTitle;
    }

    /**addPlayer
     * if the game is not full adds a player to the game and
     * increments the player counter
     * @param newPlayer the player to be added to the game
     * @return whether or not a player was added
     */

    public boolean addPlayer(String newPlayer) {
        if (numPlayers >= size) return false;
        players[numPlayers] = newPlayer;
        numPlayers++;
        return true;
    }

    /**removePlayer
     * Searches for the player in the game and removes
     * them from the game if found
     * @param player the player to remove
     * @return if the player was found or removed
     */

    public boolean removePlayer(String player) {
        for (int i = 0; i < size; i++) {
            if (players[i].equals(player)) {
                for (int j = i; j < size - 1; j++) {
                    players[j] = players[j + 1];
                }
                numPlayers--;
                return true;
            }
        }
        return false;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public boolean isFull() {
        return numPlayers >= size;
    }

    public int getSize() {
        return size;
    }

    public String getHost() {
        return players[0];
    }

    public String getTitle() {
        return title;
    }

    public String[] getPlayers() {
        return players;
    }
}