/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.validator.sentence;

import cc.redpen.RedPen;
import cc.redpen.config.Configuration;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.model.Document;
import cc.redpen.model.Sentence;
import cc.redpen.tokenizer.NeologdJapaneseTokenizer;
import cc.redpen.validator.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JapaneseAmbiguousNounConjunctionValidatorTest {
    @Test
    void testDetectJapaneseAmbiguousNounConjunction() throws Exception {
        List<Document> documents = new ArrayList<>();
        documents.add(Document.builder(new NeologdJapaneseTokenizer())
                .addSection(1)
                .addParagraph()
                .addSentence(new Sentence("弊社の経営方針の説明を受けた。", 1))
                .build());

        Configuration config = Configuration.builder("ja")
                .addValidatorConfig(new ValidatorConfiguration("JapaneseAmbiguousNounConjunction"))
                .build();

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(1, errors.get(documents.get(0)).size());
    }

    @Test
    void testNotDetectSingleJoshi() throws Exception {
        List<Document> documents = new ArrayList<>();
        documents.add(Document.builder(new NeologdJapaneseTokenizer())
                .addSection(1)
                .addParagraph()
                .addSentence(new Sentence("弊社の経営方針についての説明を受けた。", 1))
                .build());

        Configuration config = Configuration.builder("ja")
                .addValidatorConfig(new ValidatorConfiguration("JapaneseAmbiguousNounConjunction"))
                .build();

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }

    @Test
    void testUserDictionary() throws Exception {
        List<Document> documents = new ArrayList<>();
        documents.add(Document.builder(new NeologdJapaneseTokenizer())
                .addSection(1)
                .addParagraph()
                .addSentence(new Sentence("不思議の国のアリスは面白い。", 1))
                .build());

        Configuration config = Configuration.builder("ja")
                .addValidatorConfig(new ValidatorConfiguration("JapaneseAmbiguousNounConjunction").addProperty("list", "不思議の国のアリス"))
                .build();

        RedPen redPen = new RedPen(config);
        Map<Document, List<ValidationError>> errors = redPen.validate(documents);
        assertEquals(0, errors.get(documents.get(0)).size());
    }
}
