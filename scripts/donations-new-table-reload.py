"""
Module to read data from DynamoDB tables and populate into a single DynamoDB table, with logging and dry-run feature.

This module provides functions to read items from multiple DynamoDB tables that were storing donations data for
individual funds, and populate these items into a single DynamoDB table that will hold donations.
"""

import sys
import boto3
from botocore.exceptions import ClientError
import logging

def get_dynamodb_tables(dynamodb_client, exclude_table):
    """
    Get a list of DynamoDB table names, excluding a specific table.

    Args:
    dynamodb_client: A boto3 DynamoDB client.
    exclude_table: The name of the table to exclude from the list.

    Returns:
    A list of table names.
    """
    try:
        response = dynamodb_client.list_tables()
        return [table for table in response['TableNames'] if table != exclude_table]
    except ClientError as error:
        logging.error(f"Error fetching table names: {error}")
        return []

def read_table_items(dynamodb_client, table_name):
    """
    Read all items from a DynamoDB table.

    Args:
    dynamodb_client: A boto3 DynamoDB client.
    table_name: The name of the DynamoDB table.

    Returns:
    A list of items from the table.
    """
    try:
        response = dynamodb_client.scan(TableName=table_name)
        items = response['Items']
        logging.info(f"Read {len(items)} items from table '{table_name}'")
        return items
    except ClientError as error:
        logging.error(f"Error reading items from table {table_name}: {error}")
        return []

def write_items_to_table(dynamodb_client, target_table, items, original_table_name, dry_run):
    """
    Write items to a DynamoDB table with an additional attribute.

    Args:
    dynamodb_client: A boto3 DynamoDB client.
    target_table: The name of the target DynamoDB table.
    items: A list of items to write.
    original_table_name: The name of the original table to use for the 'fund_id' attribute.
    dry_run: If True, do not write to the table, only log the actions.
    """
    for item in items:
        item['fund_id'] = {'S': original_table_name}
        if not dry_run:
            try:
                dynamodb_client.put_item(TableName=target_table, Item=item)
            except ClientError as error:
                logging.error(f"Error writing item to table {target_table}: {error}")

def main(dry_run=False, region_name='us-east-1'):
    """
    Main function to migrate DynamoDB items from multiple tables to a single table.

    Args:
    dry_run: If True, do not write to the target table, only read and log the actions.
    region_name: AWS region where the DynamoDB tables are located.
    """
    dynamodb_client = boto3.client('dynamodb', region_name=region_name)
    source_tables = get_dynamodb_tables(dynamodb_client, 'funds')
    target_table = 'donations-all'

    for table_name in source_tables:
        items = read_table_items(dynamodb_client, table_name)
        write_items_to_table(dynamodb_client, target_table, items, table_name, dry_run)

if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s [%(levelname)s] %(message)s',
        stream=sys.stdout
    )
    main(dry_run=True, region_name='us-east-1')  # Set to False to execute data writing
