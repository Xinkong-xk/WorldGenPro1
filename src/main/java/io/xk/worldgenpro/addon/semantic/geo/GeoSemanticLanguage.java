package io.xk.worldgenpro.addon.semantic.geo;

public class GeoSemanticLanguage {
    public static void main(String[] args) {
        String source_2 = "edge_of(<minecraft:rever>) IS <minecraft:plains>";
        System.out.println("Input: " + source_2);

        // 1. 词法分析
        Tokenizer tokenizer = new Tokenizer(source_2);
        // 解析
        Parser parser = new Parser(tokenizer);
        GeoExpr geoExpr = parser.parseExpression();

        // 3. 打印 AST（根据你已有的 Printer 类名替换）
        String printed = new AstPrinter().print(geoExpr);

        System.out.println("\n=== Parsed AST ===");
        System.out.println(printed);
    }
}