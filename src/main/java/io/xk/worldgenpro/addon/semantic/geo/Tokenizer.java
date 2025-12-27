package io.xk.worldgenpro.addon.semantic.geo;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {

    public enum TokenType {
        AND_KW, OR_KW, IS_KW, IS_NOT_KW,
        IDENT, BIOME,
        LPAREN, RPAREN, COMMA,
        EOF
    }

    public static class Token {
        public final TokenType type;
        public final String text;
        public Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
        @Override
        public String toString() {
            return type + "(" + text + ")";
        }
    }

    private final String input;
    private int pos = 0;
    private final int length;

    public Tokenizer(String input) {
        this.input = input;
        this.length = input.length();
    }

    public TokenStream tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (pos >= length) {
                tokens.add(new Token(TokenType.EOF, ""));
                break;
            }

            char c = input.charAt(pos);

            if (c == '(') { pos++; tokens.add(new Token(TokenType.LPAREN, "(")); continue; }
            if (c == ')') { pos++; tokens.add(new Token(TokenType.RPAREN, ")")); continue; }
            if (c == ',') { pos++; tokens.add(new Token(TokenType.COMMA, ",")); continue; }

            if (c == '<') {
                int start = pos++;
                while (pos < length && input.charAt(pos) != '>') pos++;
                if (pos >= length) throw new RuntimeException("Biome missing closing '>'");
                pos++;
                tokens.add(new Token(TokenType.BIOME, input.substring(start, pos)));
                continue;
            }

            if (isLetter(c) || c == '_') {
                int start = pos;
                pos++;
                while (pos < length && (isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) pos++;
                String word = input.substring(start, pos).toUpperCase();

                switch (word) {
                    case "AND" -> { tokens.add(new Token(TokenType.AND_KW, "AND")); continue; }
                    case "OR" -> { tokens.add(new Token(TokenType.OR_KW, "OR")); continue; }
                    case "IS" -> {
                        tokens.add(new Token(TokenType.IS_KW, "IS"));
                        continue;
                    }
                    case "IS_NOT" -> {
                        tokens.add(new Token(TokenType.IS_NOT_KW, "IS_NOT"));
                        continue;
                    }
                }

                tokens.add(new Token(TokenType.IDENT, input.substring(start, pos)));
                continue;
            }

            throw new RuntimeException("Unexpected character: '" + c + "' at pos " + pos);
        }

        return new TokenStream(tokens);
    }

    private void skipWhitespace() {
        while (pos < length && Character.isWhitespace(input.charAt(pos))) pos++;
    }

    private boolean isLetter(char c) { return Character.isLetter(c); }

    private boolean isLetterOrDigit(char c) { return Character.isLetterOrDigit(c); }

    public static class TokenStream {
        private final List<Token> tokens;
        public int index = 0;
        public TokenStream(List<Token> tokens) { this.tokens = tokens; }
        public Token nextToken() {
            if (index < tokens.size()) return tokens.get(index++);
            return new Token(TokenType.EOF, "");
        }
        public Token peek() {
            if (index < tokens.size()) return tokens.get(index);
            return new Token(TokenType.EOF, "");
        }

        @Override
        public String toString() {
            return "TokenStream{" +
                    "tokens=" + tokens.toString() +
                    '}';
        }
    }
}
