/**
 * redpen: a text inspection tool
 * Copyright (C) 2014 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.unigram.docvalidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unigram.docvalidator.config.Configuration;
import org.unigram.docvalidator.config.ValidatorConfiguration;
import org.unigram.docvalidator.distributor.DefaultResultDistributor;
import org.unigram.docvalidator.distributor.ResultDistributor;
import org.unigram.docvalidator.distributor.ResultDistributorFactory;
import org.unigram.docvalidator.formatter.Formatter;
import org.unigram.docvalidator.model.Document;
import org.unigram.docvalidator.model.DocumentCollection;
import org.unigram.docvalidator.model.ListBlock;
import org.unigram.docvalidator.model.ListElement;
import org.unigram.docvalidator.model.Paragraph;
import org.unigram.docvalidator.model.Section;
import org.unigram.docvalidator.model.Sentence;
import org.unigram.docvalidator.validator.Validator;
import org.unigram.docvalidator.validator.section.SectionValidator;
import org.unigram.docvalidator.validator.section.SectionValidatorFactory;
import org.unigram.docvalidator.validator.sentence.SentenceValidator;
import org.unigram.docvalidator.validator.sentence.SentenceValidatorFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Validate all input files using appended Validators.
 */
public class DocumentValidator implements Validator {

  private DocumentValidator(Builder builder) throws DocumentValidatorException {
    Configuration configuration = builder.configuration;
    this.distributor = builder.distributor;

    validators = new ArrayList<Validator>();
    sectionValidators = new ArrayList<SectionValidator>();
    sentenceValidators = new ArrayList<SentenceValidator>();

    loadValidators(configuration);
  }

  /**
   * Load validators written in the configuration file.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private void loadValidators(Configuration configuration)
      throws DocumentValidatorException {

    //TODO duplicate code...
    for (ValidatorConfiguration config : configuration
        .getSectionValidatorConfigs()) {
      sectionValidators.add(SectionValidatorFactory
          .getInstance(config, configuration.getCharacterTable()));
    }

    for (ValidatorConfiguration config : configuration
        .getSentenceValidatorConfigs()) {
      sentenceValidators.add(SentenceValidatorFactory
          .getInstance(config, configuration.getCharacterTable()));
    }

    //TODO execute document validator
    //TODO execute paragraph validator


  }

  /**
   * Validate the input document collection.
   *
   * @param documentCollection input document collection generated by Parser
   * @return list of validation errors
   */
  public List<ValidationError> check(DocumentCollection documentCollection) {
    distributor.flushHeader();
    List<ValidationError> errors = new ArrayList<ValidationError>();
//    for (Validator validator : this.validators) {


//      Iterator<Document> fileIterator = documentCollection.getDocuments();
//      while (fileIterator.hasNext()) {
//        try {
//          List<ValidationError> currentErrors =
//            validator.validate(fileIterator.next());
//          errors.addAll(currentErrors);
//        } catch (Throwable e) {
//          LOG.error("Error occurs in validation: " + e.getMessage());
//          LOG.error("Validator class: " + validator.getClass());
//        }
//      }
//    }

    for (Document document : documentCollection) {
      errors = validateDocument(document);

      for (ValidationError error : errors){
        error.setFileName(document.getFileName());
        distributor.flushResult(error);
      }
    }

    distributor.flushFooter();
    return errors;
  }

  private List<ValidationError> validateDocument(Document document) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    for (Validator validator : validators) {
      errors.addAll(validator.validate(document));
    }

    for (Section section : document) {
      errors.addAll(validateSection(section));
    }
    return errors;
  }

  private List<ValidationError> validateSection(Section section) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    for (SectionValidator sectionValidator : sectionValidators) {
      errors.addAll(sectionValidator.validate(section));
    }

    for (Paragraph paragraph : section.getParagraphs()) {
      errors.addAll(validateParagraph(paragraph));
    }


    errors.addAll(validateSentences(section.getHeaderContents()));

    for (ListBlock listBlock : section.getListBlocks()) {
      for (ListElement listElement : listBlock.getListElements()) {
        errors.addAll(validateSentences(listElement.getSentences()));
      }

    }
    return errors;
  }

  private List<ValidationError> validateParagraph(Paragraph paragraph) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    errors.addAll(validateSentences(paragraph.getSentences()));
    return errors;
  }

  private List<ValidationError> validateSentences(List<Sentence> sentences) {
    List<ValidationError> errors = new ArrayList<ValidationError>();
    for (SentenceValidator sentenceValidator : sentenceValidators) {
      for (Sentence sentence : sentences) {
        errors.addAll(sentenceValidator.validate(sentence));
      }
    }
    return errors;
  }

  /**
   * Constructor only for testing.
   */
  protected DocumentValidator() {
    this.distributor = ResultDistributorFactory
        .createDistributor(Formatter.Type.PLAIN,
            System.out);
    this.validators = new ArrayList<Validator>();
    sectionValidators = new ArrayList<SectionValidator>();
    sentenceValidators = new ArrayList<SentenceValidator>();
  }

  /**
   * Append a specified validator.
   *
   * @param validator Validator used in testing
   */
  protected void appendValidator(Validator validator) {
    this.validators.add(validator);
  }

  @Override
  public List<ValidationError> validate(Document document) {
    return null;
  }

  public void appendSectionValidator(SectionValidator validator) {
    sectionValidators.add(validator);
  }

  /**
   * Builder for DocumentValidator.
   */
  public static class Builder {

    private Configuration configuration;

    private ResultDistributor distributor = new DefaultResultDistributor(
        new PrintStream(System.out)
    );

    public Builder setConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder setResultDistributor(ResultDistributor distributor) {
      this.distributor = distributor;
      return this;
    }

    public DocumentValidator build() throws DocumentValidatorException {
      return new DocumentValidator(this);
    }
  }

  private final List<Validator> validators;

  private final List<SectionValidator> sectionValidators;

  private final List<SentenceValidator> sentenceValidators;

  private ResultDistributor distributor;

  private static final Logger LOG =
      LoggerFactory.getLogger(DocumentValidator.class);
}
