/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.mod;

import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.smali.smaliFlexLexer;
import com.android.tools.smali.smali.smaliParser;
import com.android.tools.smali.smali.smaliTreeWalker;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SmaliMod {
    public static boolean assembleSmaliFile(File smaliFile, DexBuilder dexBuilder, int apiLevel, boolean verboseErrors,
                                            boolean printTokens) throws IOException, RecognitionException, RuntimeException {

        CommonTokenStream tokens;
        smaliFlexLexer lexer;

        InputStream is = new FileInputStream(smaliFile);
        InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

        lexer = new smaliFlexLexer(reader, apiLevel);
        (lexer).setSourceFile(smaliFile);
        tokens = new CommonTokenStream(lexer);

        if (printTokens) {
            tokens.getTokens();

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (token.getChannel() == smaliParser.HIDDEN) {
                    continue;
                }

                System.out.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
            }
        }

        smaliParser parser = new smaliParser(tokens);
        parser.setApiLevel(apiLevel);
        parser.setVerboseErrors(verboseErrors);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
            is.close();
            reader.close();
            String sourceName = lexer.getSourceName();
            String errorMsg = "Source: " + sourceName + ";Line: " + lexer.getLine() + ";Column: " + lexer.getColumn();
            throw new RuntimeException(errorMsg);
        }

        CommonTree t = result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
        dexGen.setApiLevel(apiLevel);
        dexGen.setVerboseErrors(verboseErrors);
        dexGen.setDexBuilder(dexBuilder);
        dexGen.smali_file();

        is.close();
        reader.close();

        if (dexGen.getNumberOfSyntaxErrors() != 0) {
            String sourceName = lexer.getSourceName();
            final String errorMsg = "Source: " + sourceName + ";Line: " + lexer.getLine() + ";Column: " + lexer.getColumn();
            throw new RuntimeException(errorMsg);
        }

        return dexGen.getNumberOfSyntaxErrors() == 0;
    }
}
