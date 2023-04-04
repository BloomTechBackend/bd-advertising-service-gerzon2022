package com.amazon.ata.advertising.service.businesslogic;

import com.amazon.ata.advertising.service.dao.CustomerProfileDao;
import com.amazon.ata.advertising.service.dao.ReadableDao;
import com.amazon.ata.advertising.service.model.*;
import com.amazon.ata.advertising.service.targeting.TargetingEvaluator;
import com.amazon.ata.advertising.service.targeting.TargetingGroup;

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

//
        CustomerProfileDao customerProfileDao = new CustomerProfileDao(new ATACustomerService());
        CustomerProfile customerProfile =  customerProfileDao.get(customerId);


        GeneratedAdvertisement generatedAdvertisement = new EmptyGeneratedAdvertisement();


        System.out.println("1 mississipi" + Optional.ofNullable( customerProfile.getAgeRange()).isEmpty());
        if (StringUtils.isEmpty(marketplaceId)) {
            System.out.println("2 mississipi");
            LOG.warn("MarketplaceId cannot be null or empty. Returning empty ad.");
        }  else {
            System.out.println("3 mississipi");
            final List<AdvertisementContent> contents = contentDao.get(marketplaceId);
            System.out.println(StringUtils.isEmpty(customerId) + "is this the customer empty??");
            if(Optional.ofNullable( customerProfile.getAgeRange()).isEmpty()){
                List<AdvertisementContent> emptyContents = contents.stream().map(advertisementContent -> {
                    advertisementContent.setRenderableContent("");
                    return advertisementContent;
                }).collect(Collectors.toList());
                AdvertisementContent randomAdvertisementContent = emptyContents.get(random.nextInt(emptyContents.size()));
                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
                return generatedAdvertisement;
            }


            //based on the customer being a part of an ad's targeting group
// customer is eligible base on the ad contents' targeting group
            //Use `TargetingEvaluator`
            //help filter out the ads that a customer is not eligible for
            //Then randomly return one of the ads that the customer is
            //eligible for (if any).


            System.out.println("5 mississipi");

        if (CollectionUtils.isNotEmpty(contents)) {
            System.out.println("6 mississipi");
                AdvertisementContent randomAdvertisementContent = contents.get(random.nextInt(contents.size()));
                generatedAdvertisement = new GeneratedAdvertisement(randomAdvertisementContent);
            }

        }


        return generatedAdvertisement;
    }
}
