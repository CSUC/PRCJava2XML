package org.csuc.marshal;

import org.apache.commons.text.StringEscapeUtils;
import org.csuc.global.RandomNumeric;
import org.csuc.global.Time;
import org.csuc.typesafe.semantics.ClassId;
import org.csuc.typesafe.semantics.SchemeId;
import org.csuc.typesafe.semantics.Semantics;
import xmlns.org.eurocris.cerif_1.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author amartinez
 */
public class MarshalResearchGroup extends CfOrgUnitType implements Factory{

    private ObjectFactory FACTORY = new ObjectFactory();

    private String name;
    private String sigles;
    private String url;
    private String ae;
    private String code;
    private String sgr;
    private String date;

    private List<List<Object>> relation;
    private CopyOnWriteArrayList<CfPersType> cfPersTypeList;
    private CopyOnWriteArrayList<CfPersType> newCfPersType = new CopyOnWriteArrayList<>();

    public MarshalResearchGroup(String name, String acro, String url, String ae, String code,
                                String sgr, String date, List relation, List<CfPersType> cfPersType) {

        this.name = StringEscapeUtils.escapeXml11(name);
        this.sigles = StringEscapeUtils.escapeXml11(acro);
        this.url = StringEscapeUtils.escapeXml11(url);
        this.ae = StringEscapeUtils.escapeXml11(ae);
        this.code = StringEscapeUtils.escapeXml11(code);
        this.sgr = StringEscapeUtils.escapeXml11(sgr);
        this.date = StringEscapeUtils.escapeXml11(date);

        this.relation = relation;
        this.cfPersTypeList = Objects.nonNull(cfPersType) ? new CopyOnWriteArrayList(cfPersType) : null;

        execute();
    }

    private void createAcro(){
        if(Objects.nonNull(sigles)) setCfAcro(sigles);
    }

    private void createUrl(){
        if(Objects.nonNull(url)) setCfURI(url);
    }

    private void createTitle(String langCode, CfTransType transType){
        if(Objects.nonNull(name)){
            CfMLangStringType title = new CfMLangStringType();
            title.setCfLangCode(langCode);
            title.setCfTrans(transType);
            title.setValue(name);

            getCfNameOrCfResActOrCfKeyw().add(FACTORY.createCfOrgUnitTypeCfName(title));
        }
    }

    private void createEntityClass(ClassId classId){
        CfCoreClassWithFractionType cfCoreClass = new CfCoreClassWithFractionType();
        cfCoreClass.setCfClassId(Semantics.getClassId(classId));

        cfCoreClass.setCfClassSchemeId(Semantics.getSchemaId(SchemeId.ORGANISATION_TYPES));
        getCfNameOrCfResActOrCfKeyw().add(FACTORY.createCfOrgUnitTypeCfOrgUnitClass(cfCoreClass));
    }

    private void createCode(String value, ClassId classId){
        if(Objects.nonNull(value)){
            CfFedIdEmbType fedId = new CfFedIdEmbType();

            CfCoreClassWithFractionType fedIdClass = new CfCoreClassWithFractionType();
            fedIdClass.setCfClassSchemeId(Semantics.getSchemaId(SchemeId.IDENTIFIER_TYPES));
            fedId.setCfFedId(value);
            fedIdClass.setCfClassId(Semantics.getClassId(classId));
            fedId.getCfFedIdClassOrCfFedIdSrv().add(fedIdClass);
            getCfNameOrCfResActOrCfKeyw().add(FACTORY.createCfOrgUnitTypeCfFedId(fedId));
        }
    }

    private void createEmail(){
        if(Objects.nonNull(ae)){
            Stream.of(ae.split("\\|\\|"))
                    .map(elem -> new String(elem))
                    .forEach(consumer -> {
                        CfOrgUnitType.CfOrgUnitEAddr email = new CfOrgUnitType.CfOrgUnitEAddr();
                        email.setCfEAddrId(consumer);
                        email.setCfClassId(Semantics.getClassId(ClassId.EMAIL));
                        email.setCfClassSchemeId(Semantics.getSchemaId(SchemeId.ORGANISATION_CONTACT_DETAILS));
                        getCfNameOrCfResActOrCfKeyw().add(FACTORY.createCfOrgUnitTypeCfOrgUnitEAddr(email));
                    });
        }
    }

    private void createDate(){
        if(Objects.nonNull(date)){
            CfOrgUnitSrv srv = new CfOrgUnitSrv();
            srv.setCfSrvId(RandomNumeric.getInstance().newId());
            srv.setCfClassId(Semantics.getClassId(ClassId.RESEARCH_GROUP_CREATION_DATE));
            srv.setCfClassSchemeId(Semantics.getSchemaId(SchemeId.ORGANISATION_RESEARCH_INFRASTRUCTURE_ROLES));
            srv.setCfStartDate(Time.formatDateTime(date));
            getCfNameOrCfResActOrCfKeyw().add(FACTORY.createCfOrgUnitTypeCfOrgUnitSrv(srv));
        }
    }

    private void createRelationCfPers(){
        if(Objects.nonNull(relation)){
            relation.stream().forEach(consumer->{
                if(Objects.nonNull(cfPersTypeList) && code.equals(consumer.get(0).toString())){
                    if(Objects.nonNull(consumer.get(2))){
                        CfPersType id = getIdentifier(cfPersTypeList, consumer.get(2).toString());
                        researcher(id.getCfPersId(), consumer.get(3).toString());
                    }else{
                        String random = RandomNumeric.getInstance().newId();
                        researcher(random, consumer.get(3).toString());
                        newCfPersType.add(new MarshalReseracher(random, null, null,null, consumer.get(1).toString(),
                                null, null, Semantics.getClassId(ClassId.UNCHECKED)));
                    }
                }
            });
        }
    }

    private void researcher(String id, String interve){
        CfOrgUnitType.CfPersOrgUnit persOrgUnit = new CfOrgUnitType.CfPersOrgUnit();
        persOrgUnit.setCfPersId(id);
        if (interve.toLowerCase().equals("si")
                || interve.toLowerCase().equals("s")) {
            persOrgUnit.setCfClassId(Semantics.getClassId(ClassId.GROUP_LEADER));
            persOrgUnit.setCfClassSchemeId(Semantics.getSchemaId(SchemeId.PERSON_ORGANISATION_ROLES));
        } else if (interve.toLowerCase().equals("no")
                || interve.toLowerCase().equals("n")) {
            persOrgUnit.setCfClassId(Semantics.getClassId(ClassId.MEMBER));
            persOrgUnit.setCfClassSchemeId(Semantics.getSchemaId(SchemeId.PERSON_ORGANISATION_ROLES));
        }
        getCfNameOrCfResActOrCfKeyw().add(FACTORY.createCfOrgUnitTypeCfPersOrgUnit(persOrgUnit));
    }

    @Override
    public void execute() {
        setCfOrgUnitId(RandomNumeric.getInstance().newId());

        createAcro();
        createUrl();
        createTitle("ca", CfTransType.O);
        createEntityClass(ClassId.RESEARCH_GROUP);
        createCode(code, ClassId.RESEARCH_GROUP_INTERNAL_CODE);
        createCode(sgr, ClassId.REGIONAL_RESEARCH_GROUP_CODE);
        createEmail();
        createDate();

        createRelationCfPers();
    }

    private CfPersType getIdentifier(List<CfPersType> cfPersTypeList, String value) {
        return cfPersTypeList.stream().filter(cfPersType ->
                cfPersType.getCfResIntOrCfKeywOrCfPersPers()
                        .stream().filter(f -> f.getDeclaredType().equals(CfFedIdEmbType.class))
                        .map(m -> (CfFedIdEmbType) m.getValue())
                        .filter(f -> f.getCfFedId().equals(value)) != null).findFirst().orElse(null);
    }

    public ArrayList<CfPersType> getNewCfPersType() {
        return newCfPersType.stream().collect(Collectors.toCollection(ArrayList::new));
    }
}