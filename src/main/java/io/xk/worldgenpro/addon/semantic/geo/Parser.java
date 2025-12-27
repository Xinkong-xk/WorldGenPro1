package io.xk.worldgenpro.addon.semantic.geo;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private final Tokenizer.TokenStream stream;
    private Tokenizer.Token lookahead;

    public Parser(Tokenizer tokenizer) {
        this.stream = tokenizer.tokenize();
        this.lookahead = stream.nextToken();
    }

    private Tokenizer.Token cur() {
        return lookahead;
    }

    private void advance() {
        lookahead = stream.nextToken();
    }

    private boolean accept(Tokenizer.TokenType type) {
        if (cur().type == type) {
            advance();
            return true;
        }
        return false;
    }

    private Tokenizer.Token expect(Tokenizer.TokenType type, String err) {
        if (cur().type != type) {
            throw new RuntimeException(err + " Found: " + cur());
        }
        Tokenizer.Token t = cur();
        advance();
        return t;
    }

    /* ================== 顶层入口 ================== */
    public GeoExpr parseExpression() {
        GeoExpr expr = parseOr();
        if (cur().type != Tokenizer.TokenType.EOF) {
            throw new RuntimeException("Trailing tokens after expression: " + cur());
        }
        return expr;
    }

    /* OR 层 */
    private GeoExpr parseOr() {
        GeoExpr left = parseAnd();
        while (cur().type == Tokenizer.TokenType.OR_KW) {
            advance();
            GeoExpr right = parseAnd();
            left = new GeoExpr.BinaryGeoExpr("OR", left, right);
        }
        return left;
    }

    /* AND 层 */
    private GeoExpr parseAnd() {
        GeoExpr left = parsePrimary();
        while (cur().type == Tokenizer.TokenType.AND_KW) {
            advance();
            GeoExpr right = parsePrimary();
            left = new GeoExpr.BinaryGeoExpr("AND", left, right);
        }
        return left;
    }

    private GeoExpr parsePrimary() {
        Tokenizer.Token t = cur();

        if (t.type == Tokenizer.TokenType.EOF) {
            throw new RuntimeException("Unexpected end of input");
        }

        GeoExpr primary;

        // 括号表达式
        if (t.type == Tokenizer.TokenType.LPAREN) {
            advance();
            primary = parseOr();
            expect(Tokenizer.TokenType.RPAREN, "Expected ')'");
        }
        // IS / IS_NOT 作为前缀
        else if (t.type == Tokenizer.TokenType.IS_KW || t.type == Tokenizer.TokenType.IS_NOT_KW) {
            boolean isNot = t.type == Tokenizer.TokenType.IS_NOT_KW;
            advance();
            Tokenizer.Token b = expect(Tokenizer.TokenType.BIOME, "Expected <biome> after IS/IS_NOT");
            GeoExpr biomeExpr = new GeoExpr.BiomeGeoExpr(stripAngle(b.text));
            primary = isNot ? new GeoExpr.IsNotBiomeGeoExpr(null, (GeoExpr.BiomeGeoExpr) biomeExpr)
                    : new GeoExpr.IsBiomeGeoExpr(null, (GeoExpr.BiomeGeoExpr) biomeExpr);
        }
        // bare <biome>
        else if (t.type == Tokenizer.TokenType.BIOME) {
            advance();
            primary = new GeoExpr.BiomeGeoExpr(stripAngle(t.text));
        }
        // predicate(...)
        else if (t.type == Tokenizer.TokenType.IDENT) {
            String name = t.text;
            advance();
            List<GeoExpr> args = new ArrayList<>();
            if (accept(Tokenizer.TokenType.LPAREN)) {
                if (cur().type != Tokenizer.TokenType.RPAREN) {
                    args.add(parseOr());
                    while (accept(Tokenizer.TokenType.COMMA)) {
                        args.add(parseOr());
                    }
                }
                expect(Tokenizer.TokenType.RPAREN, "Expected ')' in predicate args");
            }
            primary = new GeoExpr.PredicateCallGeoExpr(name, args);
        } else {
            throw new RuntimeException("Unexpected token in primary: " + t);
        }

        // 后缀 IS / IS_NOT
        if (cur().type == Tokenizer.TokenType.IS_KW || cur().type == Tokenizer.TokenType.IS_NOT_KW) {
            boolean isNot = cur().type == Tokenizer.TokenType.IS_NOT_KW;
            advance();
            Tokenizer.Token b = expect(Tokenizer.TokenType.BIOME, "Expected <biome> after IS/IS_NOT");
            GeoExpr.BiomeGeoExpr biomeExpr = new GeoExpr.BiomeGeoExpr(stripAngle(b.text));
            primary = isNot ? new GeoExpr.IsNotBiomeGeoExpr(primary, biomeExpr)
                    : new GeoExpr.IsBiomeGeoExpr(primary, biomeExpr);
        }

        return primary;
    }

    private static String stripAngle(String s) {
        if (s.startsWith("<") && s.endsWith(">"))
            return s.substring(1, s.length() - 1).trim();
        return s;
    }
}
