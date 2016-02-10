package equity.poker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

import gnu.trove.map.TIntDoubleMap;

public class Main{

    public static int threads = Runtime.getRuntime().availableProcessors();
    static {
        if (threads <= 0)
            threads = 1;
    }
 
    public static void main(String[] args) throws IOException {
        Enumerator.init();
        String[] board = "Qd 5s Ac 9c 5h".split(" ");
        String[] myCards = "Tc As 6s 8h".split(" ");
        String[] opponentCards = "Kc 7c 5d Jh".split(" ");
       
        System.out.println("My equity is " + getEquity(board, myCards, 1000));
        System.out.println("My percentile is " + convertEquityToPercentile(getEquity(board, myCards, 1000), 5));
        System.out.println("Opponent equity is " + getEquity(board, opponentCards, 1000));
    }

    /**
     * Get the equity, same as below except default numSimulations per core of 500
     * @param board
     * @param myCards
     * @return
     */
    public static double getEquity(String[] board, String[] myCards){
        return getEquity(board, myCards, 5000);
    }

    /**
     * Gets the equity of your own set of cards
     * @param board Known board cards (Must have either 0, 3, 4, or 5 cards)
     * @param myCards Your own set of cards (Must have 4 cards)
     * @param numSimulations If equal to 0, it enums every possibility, else sets # of simulations per core
     * @return Equity
     */
    public static double getEquity(String[] board, String[] myCards, int numSimulations){
        if (board.length == 0){
            long cardSerial = Enumerator.cardMap.get(myCards[0]);
            cardSerial = cardSerial | Enumerator.cardMap.get(myCards[1]);
            cardSerial = cardSerial | Enumerator.cardMap.get(myCards[2]);
            cardSerial = cardSerial | Enumerator.cardMap.get(myCards[3]);
            return Enumerator.startingHandEquityMap.get(cardSerial);
        }

        Enumerator[] enumerators = new Enumerator[threads];
        for (int i = 0; i < enumerators.length; i++) {
            enumerators[i] = new Enumerator(i, threads, myCards, board, numSimulations);
            enumerators[i].start();
        }
        for (Enumerator enumerator : enumerators) {
            try {
                enumerator.join();
            } catch (InterruptedException never) {}
        }
        long wins = 0; //sum up results from different threads from players
        long splits = 0;
        long losses = 0;
        for (Enumerator e : enumerators){
            wins += e.wins;
            splits += e.splits;
            losses += e.losses;
        }
        
        return (wins + splits/2.0) / (wins + splits + losses);
    }
    
    /**
     * Converts equity to percentile
     * @param equity 
     * @param turn 0 = preflop, 3 = flop, 4 = turn, 5 = river
     */
    public static double convertEquityToPercentile(double equity, int turn){
        TIntDoubleMap map;
        switch (turn){
        case 0:
            map = Enumerator.equityPreFlopPercentileMap;
            break;
        case 3:
            map = Enumerator.equityFlopPercentileMap;
            break;
        case 4:
            map = Enumerator.equityTurnPercentileMap;
            break;
        case 5:
            map = Enumerator.equityRiverPercentileMap;
            break;
        default:
            return 0.5;
        }
        
        if (equity <= 0 || equity >= 1)
            return equity;
        
        equity *= 100;
        int intEquity = (int) equity;
        double remaining = equity - intEquity;
        
        return (1-remaining)*map.get(intEquity) + (remaining)*map.get(intEquity+1);
    }
}



