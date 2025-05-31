//package com.dailycodework.pz_4_1;
//
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import java.util.Base64;
//
//public class JwtSecretGenerator {
//    public static void main(String[] args) {
//        // Генерує безпечний ключ для HS512
//        String secretString = Base64.getEncoder().encodeToString(Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded());
//        System.out.println("Ваш новий JWT секретний ключ: " + secretString);
//    }
//}
