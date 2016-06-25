/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;


import java.util.Random;
import java.util.stream.IntStream;

public class RandomString {
    public static void main(String[] args) {
        Random random = new Random();
        IntStream.range(0,10).
                forEach(i->System.out.println(generateRandomString(random, 9)));
    }

    public static String generateRandomString(Random random, int length){
        return random.ints(48,122)
                .filter(i-> (i<57 || i>65) && (i <90 || i>97))
                .mapToObj(i -> (char) i)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}