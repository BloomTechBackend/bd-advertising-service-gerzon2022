package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.CustomerProfileDao;
import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.*;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;
import com.amazon.ata.customerservice.CustomerProfile;
import com.amazon.ata.customerservice.GetCustomerProfileRequest;
import com.amazon.ata.customerservice.GetCustomerProfileResponse;
import com.amazon.atacustomerservicelambda.service.ATACustomerService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

/**
 * This class is responsible for picking the advertisement to be rendered.
 */
public class AdvertisementSelectionLogic {

    private static final Logger LOG = LogManager.getLogger(AdvertisementSelectionLogic.class);

    private final ReadableDao<String, List<AdvertisementContent>> contentDao;
    private final ReadableDao<String, List<TargetingGroup>> targetingGroupDao;
    private Random random = new Random();

    /**
     * Constructor for AdvertisementSelectionLogic.
     * @param contentDao Source of advertising content.
     * @param targetingGroupDao Source of targeting groups for each advertising content.
     */
    @Inject
    public AdvertisementSelectionLogic(ReadableDao<String, List<AdvertisementContent>> contentDao,
                                       ReadableDao<String, List<TargetingGroup>> targetingGroupDao) {
        this.contentDao = contentDao;
        this.targetingGroupDao = targetingGroupDao;

    }

    /**
     * Setter for Random class.
     * @param random generates random number used to select advertisements.
     */
    public void setRandom(Random random) {
        this.random = random;
    }

    /**
     * Gets all of the content and metadata for the marketplace and determines which content can be shown.  Returns the
     * eligible content with the highest click through rate.  If no advertisement is available or eligible, returns an
     * EmptyGeneratedAdvertisement.
     *
     * @param customerId - the customer to generate a custom advertisement for
     * @param marketplaceId - the id of the marketplace the advertisement will be rendered on
     * @return an advertisement customized for the customer id provided, or an empty advertisement if one could
     *     not be generated.
     */
    public GeneratedAdvertisement selectAdvertisement(String customerId, String marketplaceId) {

        TargetingEvaluator targetingEvaluator = new TargetingEvaluator(new RequestContext(customerId, marketplaceId));

        CustomerProfileDao customerProfileDao = new CustomerProfileDao(new ATACustomerService());
        CustomerProfile customerProfile =  customerProfileDao.get(customerId);


        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();


        if (StringUtils.isEmpty(marketplaceId)) {

            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
        }  else {

            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);

     //working
            Optional<TargetingGroup> targetingGroupsCollected  = contents.stream()
                    .flatMap(advertisementContent -> Stream.of(targetingGroupDao.get(advertisementContent.getContentId())))
                .flatMap(targetingGroups ->targetingGroups.stream()
                        .filter(targetingGroup -> targetingEvaluator.evaluate(targetingGroup).isTrue()) )
                    .sorted(Comparator.comparingDouble(TargetingGroup::getClickThroughRate)).findFirst();




           if(targetingGroupsCollected.isPresent()) {
             //  generatedAdvertisement = ;
               final List<AdvertisementContent> ligibleContents =  contents.stream()
                       .filter(advertisementContent -> advertisementContent.getContentId().equals(targetingGroupsCollected.get().getContentId()) )
                       .collect(Collectors.toList());
              generatedAdvertisement = new GeneratedAdvertisement(ligibleContents.get(random.nextInt(ligibleContents.size())));
           } else {
               return generatedAdvertisement;
           }

//
//
//
//        if (CollectionUtils.isNotEmpty(contents)) {
//
//                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
//                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
//            }

//            System.out.println("missisippi " + 3);
//            System.out.println("missisippi " +adc.isPresent() );
//            if (adc.isPresent()) {
//                //AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
//               generatedAdvertisement = new GeneratedAdvertisement(adc.get());
//            }

        }
        return generatedAdvertisement;
    }
}
