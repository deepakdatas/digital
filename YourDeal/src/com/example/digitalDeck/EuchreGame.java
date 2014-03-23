package com.example.digitalDeck;

import java.util.*;

//TODO: group puts to make more efficient
public class EuchreGame extends Game{

    private static final String[] vals = {"9", "T", "J", "Q", "K", "A"}; 
    private static final String[] suits = {"S", "C", "H", "D"};

    private int state; //An integer representation of the state of the game
    private int dealer;
    private int caller;
    private int playerTurn; //The player who's turn it is
    private int[] scores;
    private int[] tricksTaken;
    private String[] kitty;
    private String[] trick;
    private int trickIndex;
    private String topCard;
    private int trump; //The index in the suits array for trump
    private int lead; //The suit that was lead

    /**EuchreGame constructor
     * @param host the player who created the game
     * @param title the name of the game
     * Creates a game by the same means as the game constructor,
     * but also initializes Euchre class variables such as dealer and
     * scores
     */
    public EuchreGame(String host, String title) {
        super(4, host, title);
        state = 0;
        dealer = 0;
        scores[0] = 0;
        scores[1] = 0;
    }

    /**startRound
     * Shuffles the deck, deals out each players hand, the kitty,
     * and specifies the turned up card then forwards the information
     * to each player
     */
    public void startRound() {
        if (state != 0) return; //Stop from being called at the wrong time
        String hands[][] = new String[4][5];
        kitty = new String[3];
        String[] deck = makeDeck();
        topCard = deal(deck, hands);
        for (int i = 0; i < super.players.length; i++) {
            super.players[i].put("hand", hands[i]);
        }
        state = 1;
        playerTurn = dealer + 1;
        Hashtable<String, Object> update = new Hashtable<String, Object>();
        update.put("topCard", topCard);
        update.put("state", state);
        super.sender.updateGame(update);
    }

    /**chooseTrump
     * The chooseTrump method has 2 states
     * State 1: Each player recieves a query on their respective turn to call on whether
     * or not they want the suit of the top card to be trump. If a call is made this way
     * the top card is picked up by the dealer and replaces another card in the dealer's
     * hand.
     * State 2: Each player recieves a query to choose trump so long as it is not the suit
     * that was turned up.
     * In either state the calling player may choose to go alone, nulling out their partner's hand
     */
    public void chooseTrump() {
        if (state == 1) { //The initial round of trump calls
            super.players[playerTurn].put("turn", true); //Ask the player for response
            String response = queryInput("call", super.players[playerTurn]).toString();
            super.players[playerTurn].put("turn", false);
            if (!response.equalsIgnoreCase("pass")) { //If trump as called
                Boolean alone = (Boolean)queryInput("alone", super.players[playerTurn]);
                if (alone) { //If the player wants to go alone
                    int partner = playerTurn + 2;
                    if (partner > 3) partner -= 4;
                    super.players[partner].put("hand", null);
                }
                caller = playerTurn;
                String toDrop = queryInput("card", super.players[dealer]).toString(); //Query dealer what they want to do when picking up
                String[] dealerHand = (String[])super.players[dealer].get("hand");
                int index = getIndex(dealerHand, toDrop);
                dealerHand[index] = toDrop;
                super.players[dealer].put("hand", dealerHand);
                playerTurn = dealer + 1; //Reset playerTurn
                if (playerTurn > 3) playerTurn -= 4;
                state = 3;
                tricksTaken = new int[2];
                trickIndex = 0;
                lead = -1;
            } else { //If trump was not called
                if (playerTurn == dealer) state = 2;
                playerTurn++;
                if (playerTurn > 3) playerTurn -= 4;
            }
        } else if (state == 2) { //The second round of trump calls
            super.players[playerTurn].put("turn", true);
            String call = queryInput("call", super.players[playerTurn]).toString();
            super.players[playerTurn].put("turn", true);
            if (!call.equalsIgnoreCase("pass")) { //If the player called trump
                //NOTE: The turned down suit should not be selectable
                Boolean alone = (Boolean)queryInput("alone", super.players[playerTurn]);
                if (alone) { //If a loner is called
                    int partner = playerTurn + 2;
                    if (partner > 3) partner -= 4;
                    super.players[partner].put("hand", null);
                }
                trump = getIndex(suits, call);
                caller = playerTurn;
                state = 3;
                tricksTaken = new int[2];
                lead = -1;
                trickIndex = 0;
                playerTurn = dealer + 1; //Reset playerTurn
                if (playerTurn > 3) playerTurn -= 4;
            } else {
                //TODO enable screw the dealer option where the dealer may not turn trump down
                if (playerTurn == dealer) {
                    dealer++;
                    if (dealer > 3) dealer -= 4;
                    state = 0;
                    startRound();
                }
                playerTurn++;
                if (playerTurn > 3) playerTurn -= 4;
            }
        }
    }

    /**playRound
     * Takes sends a query to the player who's turn it is with a
     * list of playable cards given the situation. Once the player
     * recieves a response the card is added to the trick and the
     * playerindex advances
     */
    public void playRound() {
        if (state != 3) return;
        if (super.players[playerTurn].get("hand") == null) {
            playerTurn++;
            if (playerTurn > 3) playerTurn -= 4;
            trick[trickIndex] = null;
            trickIndex++;
            return;
        }
        //Get possible plays
        int index = 0;
        String[] hand = (String[])super.players[playerTurn].get("hand");
        String[] playable = new String[hand.length];
        for (int i = 0; i < playable.length; i++) {
            if (canPlay(hand[i])) {
                playable[index] = hand[i];
                index++;
            }
        }
        //Get player decision
        super.players[playerTurn].put("play", playable);
        String play = queryInput("card", super.players[playerTurn]).toString();
        //Update hand
        index = 0;
        String[] newHand = new String[hand.length - 1];
        for (int i = 0; i < hand.length; i++) {
            if (!hand[i].equalsIgnoreCase(play)) {
                newHand[index] = hand[i];
                index++;
            }
        }
        super.players[playerTurn].put("hand", newHand);
        //Process lead card
        if (lead == -1) {
            lead = getSuit(play);
            if (isLeft(play)) lead = trump;
        }
        //Add to trick
        trick[trickIndex] = play;
        trickIndex++;
        playerTurn++;
        if (playerTurn > 3) playerTurn -= 4;
        if (trickIndex > 3) {
            trickIndex = 0;
            state = 4;
        }
    }

    /**processTrick
     * Determines who the winner of the given trick was,
     * increments the trick count for the given team, changes
     * the player who's turn it is to the winner and continues
     * the game
     */
    public void processTrick() {
        if (state != 4) return;
        int winner = findWinner();
        tricksTaken[winner % 2]++;
        playerTurn = winner;
        if (tricksTaken[0] + tricksTaken[1] == 5) {
            state = 5;
        } else {
            state = 3;
        }
    }

    /**processDeal
     * Determines which team one the round of Euchre and
     * increments scores appropriately. If the game is over
     * the method transitions the program into a dead state
     * where information can be gathered but the game can no
     * longer be played.
     */
    public void processDeal() {
        if (state != 5) return;
        int winningTeam = 0;
        if (tricksTaken[1] >= 3) winningTeam = 1;
        //Determine scoring increment
        if (tricksTaken[winningTeam] == 5) {
            scores[winningTeam] += 2; //2 points for all 5 tricks
            if (wasLoner(winningTeam)) scores[winningTeam] += 2; //Extra 2 for loner
        } else if (caller % 2 != winningTeam) {
            scores[winningTeam] += 2; //Euch
        } else scores[winningTeam]++;
        dealer++;
        state = 0;
        if (scores[0] >= 10 || scores[1] >= 10) state = 6; //Game over
    }

    /**makeDeck
     * @return the newly made/shuffled deck
     * Makes a randomly sorted array of euchre cards
     * with no repeats containing every possible card in
     * euchre
     */
    public String[] makeDeck() {
        Random rand = new Random();
        String [] deck= new String[24];
        int val;
        int suit;
        for (int i = 0; i < 24; i++) {
            val = rand.nextInt(6);
            suit = rand.nextInt(4);
            String card = vals[val] + suits[suit];
            if (getIndex(deck, card) != -1) {
                deck[i] = card;
            } else {
                i--;
                continue;
            }
        }
        return deck;
    }

    /**deal
     * @param deck a shuffled array of euchre cards
     * @param hands an empty multidimensional array for each players hand
     * @return the turned up card at the end of the deal
     * distributes 5 cards into each players hand array, adds a 3
     * card kitty for house rules such as farmers, and turns 1 card face
     * up for the determining of trump
     */
    private String deal(String[] deck, String[][] hands) {
        int deckPtr = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                hands[i][j] = deck[deckPtr];
                deckPtr++;
            }
        }
        String topCard = deck[deckPtr];
        deckPtr++;
        for (int i = 0; i < 3; i++) {
            kitty[i] = deck[deckPtr];
            deckPtr++;
        }
        return topCard;
    }

    /**getIndex
     * @param cards the cards to search
     * @param toFind the card to find
     * @return the index of the card if any
     * Searches through an array of cards for a specific card
     * and returns the index if found, otherwise returns -1
     */
    private int getIndex(String[] cards, String toFind) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) break;
            if (cards[i].equalsIgnoreCase(toFind)) return i;
        }
        return -1;
    }

    private int getSuit(String card) {
        String suit = card.substring(1, 2);
        for (int i = 0; i < suits.length; i++) {
            if (suit.equals(suits[i])) return i;
        }
        return -1;
    }

    /**isLeft
     * @param card the card to check if it is the left
     * @return whether or not the card is the left
     * Checks to see if a given card is the jack of the
     * same color as trump
     */
    private boolean isLeft(String card) {
        String val = card.substring(0, 1);
        if (!val.equalsIgnoreCase("J")) return false;
        int suit = getSuit(card);
        if (trump % 2 == 0) {
            if (suit == trump + 1) return true;
        } else {
            if (suit == trump - 1) return true;
        }
        return false;
    }

    /**canPlay
     * @param card the card to be checked if the player can play
     * @return whether or not the card is a legal play
     * scans the current players hand to see if the passed card
     * is a legitimate play
     */
    private boolean canPlay(String card) {
        if (lead == -1) return true;
        int suit = getSuit(card);
        if (isLeft(card)) suit = trump;
        if (suit == lead) return true;
        String[] hand = (String[])super.players[playerTurn].get("hand");
        for (int i = 0; i < hand.length; i++) {
            int handSuit = getSuit(hand[i]);
            if (isLeft(hand[i])) handSuit = trump;
            if (handSuit == lead) return false;
        }
        return true;
    }

    /**findWinner
     * @return the team that won the trick
     * Scans the trick to see which card has the
     * highest value by the rules of euchre
     */
    private int findWinner() {
        int winner = -1;
        int winVal = -1;
        for (int i = 0; i < 4; i++) {
            if (trick[i] == null) continue;
            boolean isTrump = (getSuit(trick[i]) == trump);
            if (isLeft(trick[i])) isTrump = true;
            int newVal = getValue(trick[i], isTrump);
            if (newVal > winVal) {
                winVal = newVal;
                winner = i;
            }
        }
        return winner;
    }

    /**getValue
     * @param card the card to check to value of
     * @param isTrump whether or not the card was trump
     * @return an integer value of the card
     * determines a play value of a given card in a given
     * situation
     */
    private int getValue(String card, boolean isTrump) {
        if (isTrump) {
            switch(card.charAt(0)) {
                case '9':
                    return 10;
                case 'T':
                    return 11;
                case 'Q':
                    return 12;
                case 'K':
                    return 13;
                case 'A':
                    return 14;
                case 'J':
                    return 14;
            }
        } else {
            for (int i = 0; i < vals.length; i++) {
                if (card.charAt(0) == vals[i].charAt(0)) return i;
            }
        }
        return -1;
    }

    /**wasLoner
     * @param team the team to check for a loner
     * @return boolean whether the team went alone
     * checks to see if one of the players in the team
     * had a null hand which represents a loner
     */
    private boolean wasLoner(int team) {
        return (super.players[team].get("hand") == null ||
                super.players[team + 2].get("hand") == null);
    }

    private Object queryInput(String key, Player toQuery) {
        //TODO find dictionary from network stream
        //TODO return value from given key in dictionary
        return null;
    }
}
