ALTER TABLE pocket_transaction
	DROP INDEX uq_pocket_tx_source,
	ADD UNIQUE KEY uq_pocket_tx_user_source (user_id, source_type, source_key);
