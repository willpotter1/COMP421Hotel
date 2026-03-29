CREATE TABLE CampaignTargets (
	CAMPAIGN_NAME VARCHAR(50) NOT NULL,
	CID INTEGER NOT NULL,
	HOTEL_NAME VARCHAR(100) NOT NULL,
	NUM_STAYS INTEGER NOT NULL,
	TOTAL_NIGHTS INTEGER NOT NULL,
	PRIMARY KEY (CAMPAIGN_NAME, CID, HOTEL_NAME),
	FOREIGN KEY (CID) REFERENCES Customer(CID),
	FOREIGN KEY (HOTEL_NAME) REFERENCES Hotel(NAME)
)
@

CREATE OR REPLACE PROCEDURE CREATE_CAMPAIGN_TARGETS(
	IN p_campaign_name VARCHAR(50),
	IN p_hotel_name VARCHAR(100),
	IN p_start_date DATE,
	IN p_end_date DATE,
	IN p_min_stays INTEGER,
	OUT o_customers_added INTEGER
)

LANGUAGE SQL
MODIFIES SQL DATA

BEGIN ATOMIC
	DECLARE v_cid INTEGER;
	DECLARE v_num_stays INTEGER;
	DECLARE v_total_nights INTEGER;

	DECLARE at_end INTEGER DEFAULT 0;
	DECLARE not_found CONDITION FOR SQLSTATE '02000';

	DECLARE C1 CURSOR FOR
		SELECT R.CID,
		       COUNT(*) AS NUM_STAYS,
		       SUM(DAYS(R.CHECK_OUT_DATE) - DAYS(R.CHECK_IN_DATE)) AS TOTAL_NIGHTS
		FROM RESERVATION R
		WHERE R.HOTEL_NAME = p_hotel_name
		AND R.CHECK_IN_DATE BETWEEN p_start_date AND p_end_date
		AND R.CHECK_OUT_DATE > R.CHECK_IN_DATE
		GROUP BY R.CID
		HAVING COUNT(*) >= p_min_stays;
	DECLARE CONTINUE HANDLER FOR not_found
		SET at_end = 1;
	SET o_customers_added = 0;

	DELETE FROM CampaignTargets
	WHERE CAMPAIGN_NAME = p_campaign_name
	AND HOTEL_NAME = p_hotel_name;

	SET at_end = 0;

	OPEN C1;
	FETCH C1 INTO v_cid, v_num_stays, v_total_nights;

	WHILE at_end = 0 DO
		INSERT INTO CampaignTargets
			(CAMPAIGN_NAME, CID, HOTEL_NAME, NUM_STAYS, TOTAL_NIGHTS)
		VALUES
			(p_campaign_name, v_cid, p_hotel_name, v_num_stays, v_total_nights);
		SET o_customers_added = o_customers_added + 1;
		FETCH C1 INTO v_cid, v_num_stays, v_total_nights;
	END WHILE;
	CLOSE C1;
END
@
